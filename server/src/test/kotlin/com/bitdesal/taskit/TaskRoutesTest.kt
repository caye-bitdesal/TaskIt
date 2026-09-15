package com.bitdesal.taskit

import com.bitdesal.taskit.api.CreateTaskRequest
import com.bitdesal.taskit.api.PatchTaskRequest
import com.bitdesal.taskit.domain.ErrorBody
import com.bitdesal.taskit.domain.TaskDto
import com.bitdesal.taskit.domain.TaskStatus
import com.bitdesal.taskit.routes.TaskRoutes
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
import kotlin.test.assertNull

class TaskRoutesTest {
    private val taskRepository = FakeTaskRepository()
    private val releaseRepository = FakeReleaseRepository()

    private fun ApplicationTestBuilder.taskTestApp() {
        application {
            install(ServerContentNegotiation) { json() }
            routing {
                with(TaskRoutes(taskRepository, releaseRepository)) {
                    this@routing.register()
                }
            }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    @Test
    fun postTaskWithoutReleaseCreatesTodo() = testApplication {
        taskTestApp()
        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Buy milk"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<TaskDto>()
        assertEquals(TaskStatus.TODO, body.status)
        assertNull(body.releaseId)
    }

    @Test
    fun postTaskWithReleaseCreatesSelected() = testApplication {
        taskTestApp()
        val release = releaseRepository.create("v1.0", null)

        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Fix bug", releaseId = release.id))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<TaskDto>()
        assertEquals(TaskStatus.SELECTED, body.status)
        assertEquals(release.id, body.releaseId)
    }

    @Test
    fun postTaskWithInProgressWithoutReleaseReturns400() = testApplication {
        taskTestApp()
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
    fun postTaskWithUnknownReleaseReturns404() = testApplication {
        taskTestApp()
        val response = jsonClient().post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Fix bug", releaseId = 999))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("release not found", response.body<ErrorBody>().error)
    }

    @Test
    fun getTasksFiltersByReleaseAndStatus() = testApplication {
        taskTestApp()
        val release = releaseRepository.create("v1.0", null)
        taskRepository.create("Todo task", null, TaskStatus.TODO.name, null, "t1", "t1")
        taskRepository.create("Selected task", null, TaskStatus.SELECTED.name, release.id, "t2", "t2")

        val response = jsonClient().get("/tasks?releaseId=${release.id}&status=SELECTED")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<TaskDto>>()
        assertEquals(1, body.size)
        assertEquals("Selected task", body.single().title)
    }

    @Test
    fun patchTaskToInProgressWithoutReleaseReturns400() = testApplication {
        taskTestApp()
        val created = taskRepository.create(
            title = "Todo task",
            description = null,
            status = TaskStatus.TODO.name,
            releaseId = null,
            createdAt = "t1",
            updatedAt = "t1",
        )

        val response = jsonClient().patch("/tasks/${created.id}") {
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
        taskTestApp()
        val release = releaseRepository.create("v1.0", null)
        val created = taskRepository.create(
            title = "Selected task",
            description = null,
            status = TaskStatus.SELECTED.name,
            releaseId = release.id,
            createdAt = "t1",
            updatedAt = "t1",
        )

        val response = jsonClient().patch("/tasks/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody("""{"releaseId":null}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<TaskDto>()
        assertEquals(TaskStatus.TODO, body.status)
        assertNull(body.releaseId)
    }

    @Test
    fun deleteTaskReturns204() = testApplication {
        taskTestApp()
        val created = taskRepository.create(
            title = "Todo task",
            description = null,
            status = TaskStatus.TODO.name,
            releaseId = null,
            createdAt = "t1",
            updatedAt = "t1",
        )

        val response = jsonClient().delete("/tasks/${created.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNull(taskRepository.get(created.id))
    }
}
