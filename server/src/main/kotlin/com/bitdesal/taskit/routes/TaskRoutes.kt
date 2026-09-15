package com.bitdesal.taskit.routes

import com.bitdesal.taskit.api.CreateTaskRequest
import com.bitdesal.taskit.api.PatchTaskRequest
import com.bitdesal.taskit.domain.ErrorBody
import com.bitdesal.taskit.domain.TaskStatus
import com.bitdesal.taskit.repository.ReleaseRepository
import com.bitdesal.taskit.repository.TaskRepository
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class TaskRoutes @Inject constructor(
    private val taskRepository: TaskRepository,
    private val releaseRepository: ReleaseRepository,
) {
    fun Route.register() {
        route("/tasks") {
            get {
                val releaseIdParam = call.request.queryParameters["releaseId"]
                val releaseId = releaseIdParam?.toLongOrNull()
                if (releaseIdParam != null && releaseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody("Invalid param: releaseId"))
                    return@get
                }

                val statusParam = call.request.queryParameters["status"]
                val status = statusParam?.let { value ->
                    runCatching { TaskStatus.valueOf(value) }.getOrNull()
                }
                if (statusParam != null && status == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody("Invalid param: status"))
                    return@get
                }

                call.respond(taskRepository.list(releaseId = releaseId, status = status))
            }

            post {
                val body = call.receive<CreateTaskRequest>()
                if (body.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody("title is blank"))
                    return@post
                }

                val createReleaseId = body.releaseId
                if (createReleaseId != null && releaseRepository.get(createReleaseId) == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorBody("release not found"))
                    return@post
                }

                val (status, releaseId) = try {
                    TaskRules.resolveForCreate(createReleaseId, body.status)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody(e.message ?: "Bad request"))
                    return@post
                }

                val now = nowIso()
                call.respond(
                    HttpStatusCode.Created,
                    taskRepository.create(
                        title = body.title,
                        description = body.description,
                        status = status.name,
                        releaseId = releaseId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }

            get("/{id}") {
                val id = call.requireParam() ?: return@get
                val task = taskRepository.get(id)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(task)
            }

            patch("/{id}") {
                val id = call.requireParam() ?: return@patch
                val current = taskRepository.get(id)
                if (current == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@patch
                }

                val json = call.receive<JsonObject>()
                val body = Json.decodeFromJsonElement(PatchTaskRequest.serializer(), json)
                if (body.title?.isBlank() == true) {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody("title is blank"))
                    return@patch
                }

                val releaseIdPresent = "releaseId" in json
                val patchReleaseId = body.releaseId
                if (patchReleaseId != null && releaseRepository.get(patchReleaseId) == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorBody("release not found"))
                    return@patch
                }

                val (status, releaseId) = try {
                    TaskRules.resolveForPatch(
                        current = current,
                        patchStatus = body.status,
                        patchReleaseId = patchReleaseId,
                        releaseIdPresent = releaseIdPresent,
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody(e.message ?: "Bad request"))
                    return@patch
                }

                val updated = taskRepository.update(
                    id = id,
                    title = body.title ?: current.title,
                    description = body.description ?: current.description,
                    status = status.name,
                    releaseId = releaseId,
                    updatedAt = nowIso(),
                )
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@patch
                }
                call.respond(updated)
            }

            delete("/{id}") {
                val id = call.requireParam() ?: return@delete
                if (taskRepository.get(id) == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                taskRepository.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    private fun nowIso(): String = Instant.now().toString()
}
