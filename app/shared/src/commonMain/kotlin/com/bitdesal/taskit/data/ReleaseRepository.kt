package com.bitdesal.taskit.data

import com.bitdesal.taskit.api.CreateReleaseRequest
import com.bitdesal.taskit.api.PatchReleaseRequest
import com.bitdesal.taskit.domain.ReleaseDto

interface ReleaseRepository {
    suspend fun list(): List<ReleaseDto>
    suspend fun get(id: Long): ReleaseDto
    suspend fun create(request: CreateReleaseRequest): ReleaseDto
    suspend fun update(id: Long, request: PatchReleaseRequest): ReleaseDto
    suspend fun delete(id: Long)
}
