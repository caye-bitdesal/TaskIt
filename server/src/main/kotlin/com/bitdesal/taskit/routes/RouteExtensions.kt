package com.bitdesal.taskit.routes

import com.bitdesal.taskit.domain.ErrorBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Parses [paramName] from the path as [Long].
 * On failure responds with 400 and returns null so the caller can `?: return@handler`.
 */
suspend fun ApplicationCall.requireParam(paramName: String = "id"): Long? {
    val id = parameters[paramName]?.toLongOrNull()
    if (id == null) {
        respond(HttpStatusCode.BadRequest, ErrorBody("Invalid param: $paramName"))
    }
    return id
}
