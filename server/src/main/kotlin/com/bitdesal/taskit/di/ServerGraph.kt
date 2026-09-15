package com.bitdesal.taskit.di

import com.bitdesal.taskit.repository.ReleaseRepository
import com.bitdesal.taskit.repository.SqlDelightReleaseRepository
import com.bitdesal.taskit.repository.SqlDelightTaskRepository
import com.bitdesal.taskit.repository.TaskRepository
import com.bitdesal.taskit.routes.ReleaseRoutes
import com.bitdesal.taskit.routes.TaskRoutes
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(
    scope = ServerScope::class,
    bindingContainers = [DatabaseBindings::class],
)
interface ServerGraph {
    val releaseRoutes: ReleaseRoutes
    val taskRoutes: TaskRoutes
    val releaseRepository: ReleaseRepository
    val taskRepository: TaskRepository

    @Binds
    val SqlDelightReleaseRepository.bind: ReleaseRepository
    @Binds
    val SqlDelightTaskRepository.bind: TaskRepository
}