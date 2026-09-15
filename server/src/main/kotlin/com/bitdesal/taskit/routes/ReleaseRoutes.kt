package com.bitdesal.taskit.routes

import com.bitdesal.taskit.api.CreateReleaseRequest
import com.bitdesal.taskit.api.PatchReleaseRequest
import com.bitdesal.taskit.domain.ErrorBody
import com.bitdesal.taskit.repository.ReleaseRepository
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

@Inject
class ReleaseRoutes(
    private val repository: ReleaseRepository
) {
    fun Route.register() {
        route("/releases") {
            get { call.respond(repository.list()) }
            
            post { 
                val body = call.receive<CreateReleaseRequest>()
                if (body.name.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest, 
                        ErrorBody("name is blank")
                    )
                    return@post
                }
                call.respond(
                    HttpStatusCode.Created, 
                    repository.create(body.name, body.notes)
                )
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLong() ?: -1
                val result = repository.get(id)

                if (result == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                } else {
                    call.respond(result)
                }
            }

            patch("/{id}") {
                val id = call.parameters["id"]?.toLong() ?: -1
                val body = call.receive<PatchReleaseRequest>()
                val updated = repository.update(id, body.name, body.notes)

                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@patch
                } else {
                    call.respond(updated)
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLong() ?: -1
                if (repository.get(id) == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                if (repository.countTasks(id) > 0) {
                    call.respond(HttpStatusCode.Conflict, ErrorBody("release has tasks"))
                    return@delete
                }
                repository.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

