package com.bitdesal.taskit.data

import com.bitdesal.taskit.api.CreateTaskRequest
import com.bitdesal.taskit.api.PatchTaskRequest
import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus

interface TaskRepository {
    suspend fun list(releaseId: Long? = null, status: TaskStatus? = null): List<TaskDto>
    suspend fun get(id: Long): TaskDto
    suspend fun create(request: CreateTaskRequest): TaskDto
    suspend fun update(id: Long, request: PatchTaskRequest): TaskDto
    suspend fun delete(id: Long)
}
