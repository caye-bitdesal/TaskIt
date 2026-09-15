package com.bitdesal.taskit

import com.bitdesal.taskit.db.createInMemorySqlDriver
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder

fun ApplicationTestBuilder.serverTestApp() {
    application {
        module(createInMemorySqlDriver())
    }
}

fun ApplicationTestBuilder.jsonClient() = createClient {
    install(ContentNegotiation) { json() }
}
