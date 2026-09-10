package com.bitdesal.taskit.domain

import kotlinx.serialization.Serializable

@Serializable
data class ErrorBody(val error: String)
