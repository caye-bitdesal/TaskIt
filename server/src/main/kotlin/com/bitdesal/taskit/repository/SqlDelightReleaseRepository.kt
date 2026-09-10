package com.bitdesal.taskit.repository

import com.bitdesal.taskit.db.Release
import com.bitdesal.taskit.db.TaskItDatabase
import com.bitdesal.taskit.di.ServerScope
import com.bitdesal.taskit.domain.ReleaseDto
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(ServerScope::class)
@Inject
class SqlDelightReleaseRepository(
    private val db: TaskItDatabase,
) : ReleaseRepository {
    // db.releaseQueries.selectAll().executeAsList()
    override fun list(): List<ReleaseDto> {
        return db.releaseQueries.selectAll().executeAsList().map { it.toDto() }
    }

    override fun get(id: Long): ReleaseDto? {
        return db.releaseQueries.selectById(id).executeAsOneOrNull()?.toDto()
    }

    override fun create(
        name: String,
        notes: String?
    ): ReleaseDto {
        val id = db.releaseQueries.insert(
            name, 
            notes
        )
        return get(id.value) ?: error("insert failed")
    }

    override fun update(
        id: Long,
        name: String?,
        notes: String?
    ): ReleaseDto? {
        val current = get(id) ?: return null
        db.releaseQueries.update(
            name ?: current.name, 
            notes ?: current.notes, 
            id
        )
        return get(id)
    }

    override fun delete(id: Long): Boolean {
        val rowsUpdated = db.releaseQueries.delete(id).value
        return rowsUpdated > 0
    }

    override fun countTasks(releaseId: Long): Long {
        return db.taskQueries.countTasksByReleaseId(releaseId).executeAsOne()
    }
}

fun Release.toDto() = ReleaseDto(
    id = id,
    name = name,
    notes = notes,
)