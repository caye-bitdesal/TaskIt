package com.bitdesal.taskit.data

import com.bitdesal.taskit.api.CreateReleaseRequest
import com.bitdesal.taskit.api.PatchReleaseRequest
import com.bitdesal.taskit.di.AppScope
import com.bitdesal.taskit.domain.ReleaseDto
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

@SingleIn(AppScope::class)
class HttpReleaseRepository @Inject constructor(
    private val client: HttpClient,
) : ReleaseRepository {
    override suspend fun list(): List<ReleaseDto> =
        client.get("/releases").body()

    override suspend fun get(id: Long): ReleaseDto =
        client.get("/releases/$id").body()

    override suspend fun create(request: CreateReleaseRequest): ReleaseDto =
        client.post("/releases") { setBody(request) }.body()

    override suspend fun update(id: Long, request: PatchReleaseRequest): ReleaseDto =
        client.patch("/releases/$id") { setBody(request) }.body()

    override suspend fun delete(id: Long) {
        client.delete("/releases/$id")
    }
}
