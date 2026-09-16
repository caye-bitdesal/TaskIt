package com.bitdesal.taskit.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppGraphTest {
    @Test
    fun createGraphResolvesHttpClient() {
        val graph = createAppGraph()
        try {
            assertNotNull(graph.httpClient)
            assertNotNull(graph.releaseRepository)
            assertNotNull(graph.taskRepository)
            assertEquals("http://127.0.0.1:8080", graph.baseUrl)
        } finally {
            graph.httpClient.close()
        }
    }
}
