package com.bitdesal.taskit.data

import com.bitdesal.taskit.api.CreateTaskRequest
import com.bitdesal.taskit.api.PatchTaskRequest
import com.bitdesal.taskit.di.AppScope
import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

@SingleIn(AppScope::class)
@Inject
class HttpTaskRepository(
    private val client: HttpClient,
) : TaskRepository {
    override suspend fun list(releaseId: Long?, status: TaskStatus?): List<TaskDto> =
        client.get("/tasks") {
            releaseId?.let { parameter("releaseId", it) }
            status?.let { parameter("status", it.name) }
        }.body()

    override suspend fun get(id: Long): TaskDto =
        client.get("/tasks/$id").body()

    override suspend fun create(request: CreateTaskRequest): TaskDto =
        client.post("/tasks") { setBody(request) }.body()

    override suspend fun update(id: Long, request: PatchTaskRequest): TaskDto =
        client.patch("/tasks/$id") { setBody(request) }.body()

    override suspend fun delete(id: Long) {
        client.delete("/tasks/$id")
    }
}
