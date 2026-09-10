package com.bitdesal.taskit.repository

import com.bitdesal.taskit.domain.ReleaseDto

interface ReleaseRepository {
    fun list(): List<ReleaseDto>
    fun get(id: Long): ReleaseDto?
    fun create(
        name: String, 
        notes: String?
    ): ReleaseDto
    fun update(
        id: Long, 
        name: String?, 
        notes: String?
    ): ReleaseDto?
    fun delete(id: Long): Boolean
    fun countTasks(releaseId: Long): Long
}