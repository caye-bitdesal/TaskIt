package com.bitdesal.taskit.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("task")
data class TaskDto (
    val id: Long, 
    val title: String, 
    val description: String?, 
    val status: TaskStatus, 
    val releaseId: Long?, 
    val createdAt: String, 
    val updatedAt: String
)