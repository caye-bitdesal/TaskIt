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

    override fun list(): List<TaskDto> =
        db.taskQueries.selectAll().executeAsList().map { it.toDto() }

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
        title: String?,
        description: String?,
        status: String?,
        releaseId: Long?,
        updatedAt: String,
    ) {
        val current = db.taskQueries.selectById(id).executeAsOneOrNull() ?: return
        db.taskQueries.update(
            title ?: current.title,
            description ?: current.description,
            status ?: current.status,
            releaseId ?: current.releaseId,
            updatedAt,
            id,
        )
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