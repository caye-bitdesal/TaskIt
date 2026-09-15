#!/usr/bin/env python3
"""
AI-powered PR code review for TaskIt.

Runs on every PR open/update. Uses Gemini to review changed Kotlin, Gradle,
SQLDelight, and config files, then posts a structured GitHub PR review covering:
  - Bugs
  - KMP / platform correctness
  - Code understandability
  - Reusability

On subsequent pushes to the same PR the agent re-reads previous review comments
and notes which concerns have been addressed vs. which remain open.
"""

import json
import os
import subprocess
import sys
import textwrap

from google import genai
from google.genai import types
from github import Auth, Github

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

# Update to the latest available model when Google releases new ones.
# See: https://ai.google.dev/gemini-api/docs/models
GEMINI_MODEL = "gemini-3.1-pro-preview"

# Only review these file types (keeps the diff focused and tokens down)
REVIEWED_EXTENSIONS = ("*.kt", "*.kts", "*.sq", "*.xml", "*.toml")

# Hard cap on diff size sent to the model (characters, not tokens)
MAX_DIFF_CHARS = 90_000

# ---------------------------------------------------------------------------
# Prompt
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = textwrap.dedent("""\
    You are a senior Kotlin engineer performing a thorough code review for TaskIt,
    a Kotlin Multiplatform task manager. The project's tech stack is:

    - Language: Kotlin (idiomatic style required)
    - Architecture: KMP with :core (DTOs), :server (Ktor + SQLDelight + Metro),
      :app:shared (Compose Multiplatform UI + Ktor client + Metro)
    - Targets: Android, Desktop (JVM), Web (JS/Wasm); server is JVM-only
    - UI: Jetpack Compose Multiplatform with Material 3
    - Backend: Ktor REST API, JSON via kotlinx.serialization
    - Persistence: SQLDelight + SQLite on the server only (client is stateless)
    - Dependency Injection: Metro (dev.zacsweers.metro) — not Hilt, Dagger, or Koin
    - Reactivity: Kotlin Coroutines, ViewModel in shared UI

    ─────────────────────────────────────────
    REVIEW CRITERIA — comment only on these:
    ─────────────────────────────────────────
    1. BUGS
       Logic errors, null/crash risks, incorrect API usage, resource or memory leaks,
       threading violations, broken edge cases, improper lifecycle handling,
       SQL/API contract mismatches, missing validation on server routes.

    2. PLATFORM_CORRECTNESS
       Deviations from KMP, Ktor, SQLDelight, Metro, or Compose Multiplatform best practices:
       • Wrong Compose side-effect (LaunchedEffect vs SideEffect vs DisposableEffect)
       • Incorrect coroutine scope (viewModelScope vs rememberCoroutineScope)
       • Client doing persistence when the server should be the source of truth
       • Metro graph wiring mistakes (@Inject, @Binds, @Provides, scopes)
       • Ktor ContentNegotiation / status code / error body inconsistencies
       • SQLDelight schema/query mismatches with DTOs or repository mapping
       • Introducing banned DI frameworks (Hilt, Dagger, Koin)

    3. UNDERSTANDABILITY
       Unclear or misleading names, functions that do more than one thing, missing
       explanation for non-obvious logic, magic literals that should be named constants,
       deeply nested code that should be extracted.

    4. REUSABILITY
       Duplicated logic that already exists (or could be abstracted), new utilities that
       duplicate platform APIs, components that should be shared in app/shared,
       missed opportunity to generalise an existing function.

    ─────────────────────────────────────────
    PREVIOUS REVIEW COMMENTS
    ─────────────────────────────────────────
    If previous review comments are included in the prompt, determine for each one
    whether the latest commits appear to have addressed the concern. List clearly
    resolved concerns in `addressed_comments`.

    ─────────────────────────────────────────
    OUTPUT FORMAT — strict JSON, no extra text
    ─────────────────────────────────────────
    Return ONLY a JSON object matching the schema below. All arrays may be empty [].

    {
      "summary": "2–3 sentence overall assessment of the PR",
      "verdict": "approve" | "request_changes" | "comment",
      "issues": [
        {
          "file": "relative/path/to/File.kt",
          "line": 42,
          "severity": "error" | "warning" | "suggestion",
          "category": "bug" | "platform_correctness" | "understandability" | "reusability",
          "title": "Short imperative title",
          "body": "Detailed explanation and a concrete suggestion or code snippet"
        }
      ],
      "open_questions": [
        "A specific question for the PR author that needs an answer before merging?"
      ],
      "addressed_comments": [
        "Brief description of a previous review concern that the new commits resolve"
      ],
      "praise": [
        "One specific thing done well in this PR"
      ]
    }

    Use verdict="request_changes" ONLY when there is at least one issue with severity="error"
      (crash risk, data loss, security hole, broken logic). Warnings and suggestions alone
      must NOT block a merge — use verdict="comment" for those.
    Use verdict="comment" when there are only warnings, suggestions, or open questions.
    Use verdict="approve" when there are no issues at all (empty issues array).
""")

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

SEVERITY_EMOJI = {"error": "🔴", "warning": "🟡", "suggestion": "🔵"}
CATEGORY_LABEL = {
    "bug": "Bug",
    "platform_correctness": "Platform Correctness",
    "understandability": "Understandability",
    "reusability": "Reusability",
}


def run_git(cmd: str) -> str:
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout


def get_diff(base_sha: str, head_sha: str) -> str:
    extensions = " ".join(f"'{e}'" for e in REVIEWED_EXTENSIONS)
    diff = run_git(f"git diff {base_sha}...{head_sha} -- {extensions}")
    if len(diff) > MAX_DIFF_CHARS:
        diff = diff[:MAX_DIFF_CHARS] + "\n\n[diff truncated — exceeds size limit]"
    return diff


def get_previous_review_comments(pr) -> list[dict]:
    """Collect inline review comments from previous review rounds (human + bot)."""
    comments = []
    for comment in pr.get_review_comments():
        comments.append({
            "file": comment.path,
            "line": comment.original_line,
            "body": comment.body,
            "author": comment.user.login,
            "is_bot": "github-actions" in (comment.user.login or ""),
        })
    return comments


def dismiss_stale_bot_reviews(pr, final_verdict: str) -> None:
    """Dismiss previous REQUEST_CHANGES reviews posted by this bot."""
    if final_verdict == "REQUEST_CHANGES":
        return

    dismissed = 0
    for review in pr.get_reviews():
        is_bot = "github-actions" in (review.user.login or "")
        if is_bot and review.state == "CHANGES_REQUESTED":
            try:
                review.dismiss("Previous issues addressed or no longer applicable — see latest review.")
                dismissed += 1
            except Exception as exc:
                print(f"[review] Could not dismiss review {review.id}: {exc}")
    if dismissed:
        print(f"[review] Dismissed {dismissed} stale REQUEST_CHANGES review(s)")


def get_previous_issue_comments(pr) -> list[dict]:
    """Collect general (non-review) PR comments from humans."""
    comments = []
    for comment in pr.get_issue_comments():
        if "github-actions" in (comment.user.login or ""):
            continue
        comments.append({"author": comment.user.login, "body": comment.body})
    return comments


def build_user_prompt(
    diff: str,
    pr_title: str,
    pr_body: str,
    review_comments: list[dict],
    issue_comments: list[dict],
) -> str:
    parts = [
        f"## PR Title\n{pr_title}\n",
        f"## PR Description\n{pr_body or '*(no description provided)*'}\n",
        f"## Diff\n```diff\n{diff}\n```\n",
    ]

    if review_comments:
        parts.append("## Previous Inline Review Comments\n")
        for c in review_comments:
            parts.append(
                f"- **{c['file']}:{c['line']}** — {c['author']}: {c['body']}\n"
            )

    if issue_comments:
        parts.append("## Previous PR Discussion Comments\n")
        for c in issue_comments:
            parts.append(f"- **{c['author']}**: {c['body']}\n")

    return "\n".join(parts)


def call_gemini(user_prompt: str) -> dict:
    client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=user_prompt,
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_PROMPT,
            temperature=0.2,
            max_output_tokens=65536,
            response_mime_type="application/json",
        ),
    )

    candidate = response.candidates[0] if response.candidates else None
    if candidate is None:
        raise RuntimeError("Gemini returned no candidates.")

    finish_reason = candidate.finish_reason
    ok_reasons = (types.FinishReason.STOP, types.FinishReason.FINISH_REASON_UNSPECIFIED, None)
    if finish_reason not in ok_reasons:
        raise RuntimeError(
            f"Gemini stopped early with finish_reason={finish_reason}. "
            "For MAX_TOKENS: reduce MAX_DIFF_CHARS or increase max_output_tokens."
        )

    raw = response.text.strip()
    if raw.startswith("```"):
        raw = raw.split("\n", 1)[1]
        raw = raw.rsplit("```", 1)[0]
    return json.loads(raw)


def format_review_body(review: dict) -> str:
    """Convert the structured review dict into a GitHub-flavoured Markdown body."""
    lines = ["## 🤖 AI Code Review (TaskIt)\n", review["summary"], ""]

    issues = review.get("issues", [])
    if issues:
        lines.append("---\n### Issues\n")
        for issue in issues:
            emoji = SEVERITY_EMOJI.get(issue["severity"], "⚪")
            cat = CATEGORY_LABEL.get(issue["category"], issue["category"])
            location = f"`{issue['file']}`"
            if issue.get("line"):
                location += f" line {issue['line']}"
            lines.append(f"#### {emoji} {issue['title']}")
            lines.append(f"**Category:** {cat} &nbsp;|&nbsp; **Location:** {location}\n")
            lines.append(f"{issue['body']}\n")

    questions = review.get("open_questions", [])
    if questions:
        lines.append("---\n### ❓ Open Questions\n")
        for q in questions:
            lines.append(f"- {q}")
        lines.append("")

    addressed = review.get("addressed_comments", [])
    if addressed:
        lines.append("---\n### ✅ Addressed from Previous Review\n")
        for item in addressed:
            lines.append(f"- {item}")
        lines.append("")

    praise = review.get("praise", [])
    if praise:
        lines.append("---\n### 👍 Well Done\n")
        for item in praise:
            lines.append(f"- {item}")
        lines.append("")

    lines.append(
        "---\n*Generated automatically by the AI Code Review workflow using "
        f"**{GEMINI_MODEL}**. A human reviewer must still approve before merging.*"
    )
    return "\n".join(lines)


def resolve_verdict(review: dict) -> str:
    """Map the model's verdict to a GitHub event, enforcing severity rules."""
    has_errors = any(
        i.get("severity") == "error" for i in review.get("issues", [])
    )
    raw = review.get("verdict", "comment").upper()

    if raw == "REQUEST_CHANGES" and not has_errors:
        raw = "COMMENT"

    if raw not in ("APPROVE", "REQUEST_CHANGES", "COMMENT"):
        raw = "COMMENT"

    if raw == "APPROVE":
        raw = "COMMENT"

    return raw


def main() -> None:
    token = os.environ["GITHUB_TOKEN"]
    repo_name = os.environ["REPO_NAME"]
    pr_number = int(os.environ["PR_NUMBER"])
    base_sha = os.environ["BASE_SHA"]
    head_sha = os.environ["HEAD_SHA"]

    print(f"[review] PR #{pr_number} — {os.environ.get('PR_TITLE', '')}")

    diff = get_diff(base_sha, head_sha)
    if not diff.strip():
        print("[review] No relevant files changed — skipping.")
        sys.exit(0)
    print(f"[review] Diff size: {len(diff):,} chars")

    g = Github(auth=Auth.Token(token))
    repo = g.get_repo(repo_name)
    pr = repo.get_pull(pr_number)

    review_comments = get_previous_review_comments(pr)
    issue_comments = get_previous_issue_comments(pr)
    print(
        f"[review] Context: {len(review_comments)} review comment(s), "
        f"{len(issue_comments)} issue comment(s)"
    )

    prompt = build_user_prompt(
        diff, pr.title, pr.body or "", review_comments, issue_comments
    )
    print(f"[review] Sending prompt ({len(prompt):,} chars) to {GEMINI_MODEL}…")
    review = call_gemini(prompt)
    print(
        f"[review] Received verdict='{review.get('verdict')}', "
        f"{len(review.get('issues', []))} issue(s), "
        f"{len(review.get('open_questions', []))} question(s)"
    )

    final_verdict = resolve_verdict(review)
    body = format_review_body(review)

    dismiss_stale_bot_reviews(pr, final_verdict)

    pr.create_review(body=body, event=final_verdict)
    print(f"[review] Review posted (event={final_verdict})")

    questions = review.get("open_questions", [])
    if questions:
        q_body = "### ❓ Questions for the author\n\n"
        q_body += "\n".join(f"- [ ] {q}" for q in questions)
        q_body += (
            "\n\n*Please address these questions in the PR description or "
            "as reply comments before requesting re-review.*"
        )
        pr.create_issue_comment(q_body)
        print(f"[review] Posted {len(questions)} open question(s) as issue comment")


if __name__ == "__main__":
    main()
