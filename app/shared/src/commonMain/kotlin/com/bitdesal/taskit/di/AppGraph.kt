package com.bitdesal.taskit.di

import com.bitdesal.taskit.data.HttpReleaseRepository
import com.bitdesal.taskit.data.HttpTaskRepository
import com.bitdesal.taskit.data.ReleaseRepository
import com.bitdesal.taskit.data.TaskRepository
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.createGraph
import io.ktor.client.HttpClient

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [NetworkBindings::class],
)
interface AppGraph {
    val httpClient: HttpClient
    @Named("baseUrl")
    val baseUrl: String
    val releaseRepository: ReleaseRepository
    val taskRepository: TaskRepository

    @Binds
    val HttpReleaseRepository.bind: ReleaseRepository
    @Binds
    val HttpTaskRepository.bind: TaskRepository
}

fun createAppGraph(): AppGraph = createGraph<AppGraph>()
