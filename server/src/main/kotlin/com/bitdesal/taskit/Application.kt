package com.bitdesal.taskit

import app.cash.sqldelight.db.SqlDriver
import com.bitdesal.taskit.db.createSqlDriver
import com.bitdesal.taskit.di.ServerGraph
import com.bitdesal.taskit.domain.ErrorBody
import dev.zacsweers.metro.createGraphFactory
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    testableModule(createSqlDriver())
}

fun Application.testableModule(driver: SqlDriver) {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(cause.message ?: "Bad request"),
            )
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorBody(cause.message ?: "Not found"),
            )
        }
        exception<IllegalStateException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorBody(cause.message ?: "Conflict"),
            )
        }
    }

    val graph = createGraphFactory<ServerGraph.Factory>().create(driver)
    monitor.subscribe(ApplicationStopped) {
        driver.close()
    }

    routing {
        with(graph.releaseRoutes) {
            this@routing.register()
        }
        with(graph.taskRoutes) {
            this@routing.register()
        }
    }
}