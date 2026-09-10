package com.bitdesal.taskit.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus {
    TODO,
    SELECTED,
    IN_PROGRESS,
    READY,
    DONE
}