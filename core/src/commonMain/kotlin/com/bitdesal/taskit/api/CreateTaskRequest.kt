package com.bitdesal.taskit.api

import com.bitdesal.taskit.domain.TaskStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val status: TaskStatus? = null,
    val releaseId: Long? = null,
)
