package com.bitdesal.taskit.routes

import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus

object TaskRules {
    private val statusesRequiringRelease = setOf(
        TaskStatus.SELECTED,
        TaskStatus.IN_PROGRESS,
        TaskStatus.READY,
        TaskStatus.DONE,
    )

    fun statusRequiresRelease(status: TaskStatus): Boolean = status in statusesRequiringRelease

    fun validateStatusAndRelease(status: TaskStatus, releaseId: Long?) {
        if (statusRequiresRelease(status) && releaseId == null) {
            throw IllegalArgumentException("status $status requires a release")
        }
    }

    fun resolveForCreate(releaseId: Long?, status: TaskStatus?): Pair<TaskStatus, Long?> {
        val resolvedStatus = status ?: if (releaseId == null) TaskStatus.TODO else TaskStatus.SELECTED
        validateStatusAndRelease(resolvedStatus, releaseId)
        return resolvedStatus to releaseId
    }

    fun resolveForPatch(
        current: TaskDto,
        patchStatus: TaskStatus?,
        patchReleaseId: Long?,
        releaseIdPresent: Boolean,
    ): Pair<TaskStatus, Long?> {
        val releaseId = when {
            releaseIdPresent && patchReleaseId == null -> null
            patchReleaseId != null -> patchReleaseId
            else -> current.releaseId
        }

        if (patchStatus != null) {
            validateStatusAndRelease(patchStatus, releaseId)
        }

        val status = when {
            releaseId == null -> TaskStatus.TODO
            releaseIdPresent && patchReleaseId != null && patchStatus == null -> {
                when (current.status) {
                    TaskStatus.IN_PROGRESS, TaskStatus.READY, TaskStatus.DONE -> current.status
                    else -> TaskStatus.SELECTED
                }
            }
            else -> patchStatus ?: current.status
        }

        validateStatusAndRelease(status, releaseId)
        return status to releaseId
    }
}
