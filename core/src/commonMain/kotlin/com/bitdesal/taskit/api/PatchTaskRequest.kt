package com.bitdesal.taskit.api

import com.bitdesal.taskit.domain.TaskStatus
import kotlinx.serialization.Serializable

@Serializable
data class PatchTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus? = null,
    val releaseId: Long? = null,
)
