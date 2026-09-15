package com.bitdesal.taskit

import com.bitdesal.taskit.api.CreateReleaseRequest
import com.bitdesal.taskit.api.CreateTaskRequest
import com.bitdesal.taskit.api.PatchReleaseRequest
import com.bitdesal.taskit.api.PatchTaskRequest
import com.bitdesal.taskit.domain.ErrorBody
import com.bitdesal.taskit.domain.ReleaseDto
import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerApiTest {

    @Test
    fun releaseCrudlPersists() = testApplication {
        serverTestApp()
        val client = jsonClient()

        val created = client.post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "v1.0", notes = "first"))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val release = created.body<ReleaseDto>()
        assertEquals("v1.0", release.name)
        assertEquals("first", release.notes)
        assertTrue(release.id > 0)

        val listed = client.get("/releases")
        assertEquals(HttpStatusCode.OK, listed.status)
        assertEquals(listOf(release), listed.body<List<ReleaseDto>>())

        val fetched = client.get("/releases/${release.id}")
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertEquals(release, fetched.body<ReleaseDto>())

        val patched = client.patch("/releases/${release.id}") {
            contentType(ContentType.Application.Json)
            setBody(PatchReleaseRequest(name = "v1.1", notes = "updated"))
        }
        assertEquals(HttpStatusCode.OK, patched.status)
        val updated = patched.body<ReleaseDto>()
        assertEquals("v1.1", updated.name)
        assertEquals("updated", updated.notes)

        val deleted = client.delete("/releases/${release.id}")
        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/releases/${release.id}").status)
    }

    @Test
    fun postReleaseWithBlankNameReturns400() = testApplication {
        serverTestApp()
        val response = jsonClient().post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "   "))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("name is blank", response.body<ErrorBody>().error)
    }

    @Test
    fun getReleaseReturns404WhenMissing() = testApplication {
        serverTestApp()
        val response = jsonClient().get("/releases/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun deleteReleaseReturns409WhenReleaseHasTasks() = testApplication {
        serverTestApp()
        val client = jsonClient()
        val release = client.post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "v1.0"))
        }.body<ReleaseDto>()

        val task = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Selected", releaseId = release.id))
        }
        assertEquals(HttpStatusCode.Created, task.status)

        val deleted = client.delete("/releases/${release.id}")
        assertEquals(HttpStatusCode.Conflict, deleted.status)
        assertEquals("release has tasks", deleted.body<ErrorBody>().error)
        assertEquals(HttpStatusCode.OK, client.get("/releases/${release.id}").status)
    }

    @Test
    fun postTaskWithoutReleaseCreatesTodo() = testApplication {
        serverTestApp()
        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Buy milk"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<TaskDto>()
        assertEquals("Buy milk", body.title)
        assertEquals(TaskStatus.TODO, body.status)
        assertNull(body.releaseId)
        assertTrue(body.createdAt.isNotBlank())
        assertTrue(body.updatedAt.isNotBlank())
    }

    @Test
    fun postTaskWithReleaseCreatesSelected() = testApplication {
        serverTestApp()
        val client = jsonClient()
        val release = client.post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "v1.0"))
        }.body<ReleaseDto>()

        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Fix bug", releaseId = release.id))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<TaskDto>()
        assertEquals(TaskStatus.SELECTED, body.status)
        assertEquals(release.id, body.releaseId)
    }

    @Test
    fun postTaskWithBlankTitleReturns400() = testApplication {
        serverTestApp()
        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "   "))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("title is blank", response.body<ErrorBody>().error)
    }

    @Test
    fun postTaskWithUnknownReleaseReturns400() = testApplication {
        serverTestApp()
        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Fix bug", releaseId = 999))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("release not found", response.body<ErrorBody>().error)
    }

    @Test
    fun postTaskInProgressWithoutReleaseReturns400() = testApplication {
        serverTestApp()
        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Fix bug", status = TaskStatus.IN_PROGRESS))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "status IN_PROGRESS requires a release",
            response.body<ErrorBody>().error,
        )
    }

    @Test
    fun getTasksFiltersByReleaseAndStatus() = testApplication {
        serverTestApp()
        val client = jsonClient()
        val release = client.post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "v1.0"))
        }.body<ReleaseDto>()
        client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Todo task"))
        }
        client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Selected task", releaseId = release.id))
        }

        val response = client.get("/tasks?releaseId=${release.id}&status=SELECTED")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<TaskDto>>()
        assertEquals(1, body.size)
        assertEquals("Selected task", body.single().title)
    }

    @Test
    fun getTaskReturns404WhenMissing() = testApplication {
        serverTestApp()
        val response = jsonClient().get("/tasks/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun patchTaskToInProgressWithoutReleaseReturns400() = testApplication {
        serverTestApp()
        val client = jsonClient()
        val created = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Todo task"))
        }.body<TaskDto>()

        val response = client.patch("/tasks/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(PatchTaskRequest(status = TaskStatus.IN_PROGRESS))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "status IN_PROGRESS requires a release",
            response.body<ErrorBody>().error,
        )
    }

    @Test
    fun patchTaskClearingReleaseForcesTodo() = testApplication {
        serverTestApp()
        val client = jsonClient()
        val release = client.post("/releases") {
            contentType(ContentType.Application.Json)
            setBody(CreateReleaseRequest(name = "v1.0"))
        }.body<ReleaseDto>()
        val created = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Selected task", releaseId = release.id))
        }.body<TaskDto>()

        val response = client.patch("/tasks/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("releaseId", JsonNull) })
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<TaskDto>()
        assertEquals(TaskStatus.TODO, body.status)
        assertNull(body.releaseId)
    }

    @Test
    fun deleteTaskReturns204AndRemovesFromList() = testApplication {
        serverTestApp()
        val client = jsonClient()
        val created = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Todo task"))
        }.body<TaskDto>()

        val deleted = client.delete("/tasks/${created.id}")
        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/tasks/${created.id}").status)
        assertEquals(emptyList<TaskDto>(), client.get("/tasks").body())
    }
}
