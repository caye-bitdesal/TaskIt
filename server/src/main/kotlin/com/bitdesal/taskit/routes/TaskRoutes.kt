package com.bitdesal.taskit.routes

import com.bitdesal.taskit.repository.TaskRepository
import dev.zacsweers.metro.Inject
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

@Inject
class TaskRoutes(
    val repository: TaskRepository
) {
    fun Route.register() {
        route("tasks") {
            get { call.respond(repository.list()) }
        }
    }
}