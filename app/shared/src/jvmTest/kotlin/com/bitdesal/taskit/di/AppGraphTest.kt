package com.bitdesal.taskit.di

import com.bitdesal.taskit.config.AppConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppGraphTest {
    @Test
    fun createGraphResolvesHttpClient() {
        val graph = createAppGraph()
        try {
            assertNotNull(graph.httpClient)
            assertNotNull(graph.releaseRepository)
            assertNotNull(graph.taskRepository)
            assertTrue(graph.baseUrl.isNotBlank(), "baseUrl must be configured")
            assertEquals(AppConfig.BASE_URL, graph.baseUrl)
        } finally {
            graph.httpClient.close()
        }
    }
}
