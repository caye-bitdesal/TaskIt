package com.bitdesal.taskit.repository

import com.bitdesal.taskit.db.Task
import com.bitdesal.taskit.db.TaskItDatabase
import com.bitdesal.taskit.di.ServerScope
import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(ServerScope::class)
@Inject
class SqlDelightTaskRepository(
    private val db: TaskItDatabase,
) : TaskRepository {

    override fun list(
        releaseId: Long?,
        status: TaskStatus?,
    ): List<TaskDto> =
        db.taskQueries.selectAll().executeAsList()
            .map { it.toDto() }
            .filter { task ->
                (releaseId == null || task.releaseId == releaseId) &&
                    (status == null || task.status == status)
            }

    override fun get(id: Long): TaskDto? =
        db.taskQueries.selectById(id).executeAsOneOrNull()?.toDto()

    override fun listByRelease(releaseId: Long): List<TaskDto> =
        db.taskQueries.tasksByReleaseId(releaseId).executeAsList().map { it.toDto() }

    override fun create(
        title: String,
        description: String?,
        status: String,
        releaseId: Long?,
        createdAt: String,
        updatedAt: String,
    ): TaskDto {
        val id = db.taskQueries.insert(
            title,
            description,
            status,
            releaseId,
            createdAt,
            updatedAt,
        )
        return get(id.value) ?: error("insert failed")
    }

    override fun update(
        id: Long,
        title: String,
        description: String?,
        status: String,
        releaseId: Long?,
        updatedAt: String,
    ): TaskDto? {
        if (db.taskQueries.selectById(id).executeAsOneOrNull() == null) return null
        db.taskQueries.update(
            title,
            description,
            status,
            releaseId,
            updatedAt,
            id,
        )
        return get(id)
    }

    override fun delete(id: Long): Boolean =
        db.taskQueries.delete(id).value > 0
}

fun Task.toDto() = TaskDto(
    id = id,
    title = title,
    description = description,
    status = TaskStatus.valueOf(status),
    releaseId = releaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)