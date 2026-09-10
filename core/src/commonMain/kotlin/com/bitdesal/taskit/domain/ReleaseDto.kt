package com.bitdesal.taskit.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("release")
data class ReleaseDto(
    val id: Long, 
    val name: String, 
    val notes: String?
)
