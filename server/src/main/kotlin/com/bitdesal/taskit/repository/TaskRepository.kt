package com.bitdesal.taskit.repository

import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus

interface TaskRepository {
    fun list(releaseId: Long? = null, status: TaskStatus? = null): List<TaskDto>
    fun get(id: Long): TaskDto?
    fun listByRelease(releaseId: Long): List<TaskDto>
    fun create(
        title: String,
        description: String?,
        status: String,
        releaseId: Long?,
        createdAt: String,
        updatedAt: String,
    ): TaskDto
    fun update(
        id: Long,
        title: String,
        description: String?,
        status: String,
        releaseId: Long?,
        updatedAt: String,
    ): TaskDto?
    fun delete(id: Long): Boolean
}