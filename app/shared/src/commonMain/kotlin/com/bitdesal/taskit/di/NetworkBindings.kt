package com.bitdesal.taskit.di

import com.bitdesal.taskit.config.AppConfig
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@BindingContainer
object NetworkBindings {
    @Provides
    @Named("baseUrl")
    fun baseUrl(): String = AppConfig.BASE_URL

    @Provides
    @SingleIn(AppScope::class)
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @SingleIn(AppScope::class)
    fun httpClient(
        json: Json,
        @Named("baseUrl") baseUrl: String,
    ): HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            url(baseUrl)
        }
    }
}
