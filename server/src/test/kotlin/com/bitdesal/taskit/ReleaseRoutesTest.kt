package com.bitdesal.taskit

import com.bitdesal.taskit.api.CreateReleaseRequest
import com.bitdesal.taskit.api.PatchReleaseRequest
import com.bitdesal.taskit.domain.ErrorBody
import com.bitdesal.taskit.domain.ReleaseDto
import com.bitdesal.taskit.routes.ReleaseRoutes
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseRoutesTest {
    private val repository = FakeReleaseRepository()

    private fun ApplicationTestBuilder.releaseTestApp() {
        application {
            install(ServerContentNegotiation) { json() }
            routing {
                with(ReleaseRoutes(repository)) {
                    this@routing.register()
                }
            }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    @Test
    fun getReleasesReturnsEmptyList() = testApplication {
        releaseTestApp()
        val response = jsonClient().get("/releases")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList<ReleaseDto>(), response.body())
    }

    @Test
    fun postReleaseCreatesAndReturns201() = testApplication {
        releaseTestApp()
        val response = jsonClient().post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "v1.0", notes = "first"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<ReleaseDto>()
        assertEquals("v1.0", body.name)
        assertEquals("first", body.notes)
        assertTrue(body.id > 0)
    }

    @Test
    fun postReleaseWithBlankNameReturns400() = testApplication {
        releaseTestApp()
        val response = jsonClient().post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "   "))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("name is blank", response.body<ErrorBody>().error)
    }

    @Test
    fun getReleaseByIdReturnsRelease() = testApplication {
        releaseTestApp()
        val created = repository.create("v1.0", null)

        val response = jsonClient().get("/releases/${created.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(created, response.body<ReleaseDto>())
    }

    @Test
    fun getReleaseByIdReturns404WhenMissing() = testApplication {
        releaseTestApp()
        val response = jsonClient().get("/releases/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun getReleaseByIdReturns400WhenIdIsInvalid() = testApplication {
        releaseTestApp()
        val response = jsonClient().get("/releases/not-a-number")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid param: id", response.body<ErrorBody>().error)
    }

    @Test
    fun patchReleaseUpdatesFields() = testApplication {
        releaseTestApp()
        val created = repository.create("old", "old notes")

        val response = jsonClient().patch("/releases/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(PatchReleaseRequest(name = "new", notes = "new notes"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<ReleaseDto>()
        assertEquals("new", body.name)
        assertEquals("new notes", body.notes)
    }

    @Test
    fun patchReleaseReturns404WhenMissing() = testApplication {
        releaseTestApp()
        val response = jsonClient().patch("/releases/999") {
            contentType(ContentType.Application.Json)
            setBody(PatchReleaseRequest(name = "x"))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun deleteReleaseReturns204() = testApplication {
        releaseTestApp()
        val created = repository.create("v1.0", null)

        val response = jsonClient().delete("/releases/${created.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNull(repository.get(created.id))
    }

    @Test
    fun deleteReleaseReturns409WhenReleaseHasTasks() = testApplication {
        releaseTestApp()
        val created = repository.create("v1.0", null)
        repository.setTaskCount(created.id, 2)

        val response = jsonClient().delete("/releases/${created.id}")
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("release has tasks", response.body<ErrorBody>().error)
        assertNotNull(repository.get(created.id))
    }

    @Test
    fun deleteReleaseReturns404WhenMissing() = testApplication {
        releaseTestApp()
        val response = jsonClient().delete("/releases/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
