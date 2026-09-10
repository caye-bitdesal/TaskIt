package com.bitdesal.taskit.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateReleaseRequest(
    val name: String,
    val notes: String? = null
)
