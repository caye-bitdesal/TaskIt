package com.bitdesal.taskit

import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus
import com.bitdesal.taskit.repository.TaskRepository

class FakeTaskRepository : TaskRepository {
    private val tasks = linkedMapOf<Long, TaskDto>()
    private var nextId = 1L

    override fun list(releaseId: Long?, status: TaskStatus?): List<TaskDto> =
        tasks.values.filter { task ->
            (releaseId == null || task.releaseId == releaseId) &&
                (status == null || task.status == status)
        }

    override fun get(id: Long): TaskDto? = tasks[id]

    override fun listByRelease(releaseId: Long): List<TaskDto> = list(releaseId = releaseId)

    override fun create(
        title: String,
        description: String?,
        status: String,
        releaseId: Long?,
        createdAt: String,
        updatedAt: String,
    ): TaskDto {
        val dto = TaskDto(
            id = nextId++,
            title = title,
            description = description,
            status = TaskStatus.valueOf(status),
            releaseId = releaseId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
        tasks[dto.id] = dto
        return dto
    }

    override fun update(
        id: Long,
        title: String,
        description: String?,
        status: String,
        releaseId: Long?,
        updatedAt: String,
    ): TaskDto? {
        val current = tasks[id] ?: return null
        val updated = current.copy(
            title = title,
            description = description,
            status = TaskStatus.valueOf(status),
            releaseId = releaseId,
            updatedAt = updatedAt,
        )
        tasks[id] = updated
        return updated
    }

    override fun delete(id: Long): Boolean = tasks.remove(id) != null
}
