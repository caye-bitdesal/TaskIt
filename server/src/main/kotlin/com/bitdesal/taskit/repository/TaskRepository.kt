package com.bitdesal.taskit.repository

import com.bitdesal.taskit.db.Task
import com.bitdesal.taskit.domain.TaskDto

interface TaskRepository {
    fun list(): List<TaskDto>
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
        title: String?,
        description: String?,
        status: String?,
        releaseId: Long?,
        updatedAt: String,
    )
    fun delete(id: Long): Boolean
}