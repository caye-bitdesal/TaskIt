package com.bitdesal.taskit.api

import kotlinx.serialization.Serializable

@Serializable
data class PatchReleaseRequest(
    val name: String? = null,
    val notes: String? = null,
)
