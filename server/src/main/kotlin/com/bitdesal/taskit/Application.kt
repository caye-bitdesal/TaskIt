package com.bitdesal.taskit

import com.bitdesal.taskit.di.ServerGraph
import dev.zacsweers.metro.createGraph
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val graph = createGraph<ServerGraph>()

    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
    }
}