package com.bitdesal.taskit

import com.bitdesal.taskit.domain.ReleaseDto
import com.bitdesal.taskit.repository.ReleaseRepository

class FakeReleaseRepository : ReleaseRepository {
    private val releases = linkedMapOf<Long, ReleaseDto>()
    private var nextId = 1L
    private val taskCounts = mutableMapOf<Long, Long>()

    override fun list(): List<ReleaseDto> = releases.values.toList()

    override fun get(id: Long): ReleaseDto? = releases[id]

    override fun create(name: String, notes: String?): ReleaseDto {
        val dto = ReleaseDto(id = nextId++, name = name, notes = notes)
        releases[dto.id] = dto
        return dto
    }

    override fun update(id: Long, name: String?, notes: String?): ReleaseDto? {
        val current = releases[id] ?: return null
        val updated = current.copy(
            name = name ?: current.name,
            notes = notes ?: current.notes,
        )
        releases[id] = updated
        return updated
    }

    override fun delete(id: Long): Boolean = releases.remove(id) != null

    override fun countTasks(releaseId: Long): Long = taskCounts[releaseId] ?: 0L

    fun setTaskCount(releaseId: Long, count: Long) {
        taskCounts[releaseId] = count
    }
}
