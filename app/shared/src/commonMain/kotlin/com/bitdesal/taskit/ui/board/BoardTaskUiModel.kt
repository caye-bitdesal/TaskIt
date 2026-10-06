package com.bitdesal.taskit.ui.board

import com.bitdesal.taskit.domain.TaskStatus

data class BoardTaskUiModel(
    val id: Long,
    val title: String,
    val releaseName: String?,
    val status: TaskStatus,
)

internal fun sampleBoardTasks(): List<BoardTaskUiModel> = listOf(
    BoardTaskUiModel(1, "Draft project roadmap", null, TaskStatus.TODO),
    BoardTaskUiModel(2, "Review API contract", null, TaskStatus.TODO),
    BoardTaskUiModel(3, "Fix login redirect", "v1.2", TaskStatus.SELECTED),
    BoardTaskUiModel(4, "Add dark theme toggle", "v1.2", TaskStatus.SELECTED),
    BoardTaskUiModel(5, "Implement board columns", "v1.2", TaskStatus.IN_PROGRESS),
    BoardTaskUiModel(6, "Wire task editor form", "v1.3", TaskStatus.IN_PROGRESS),
    BoardTaskUiModel(7, "QA release checklist", "v1.1", TaskStatus.READY),
    BoardTaskUiModel(8, "Update store listing", "v1.1", TaskStatus.READY),
    BoardTaskUiModel(9, "Ship initial MVP", "v1.0", TaskStatus.DONE),
    BoardTaskUiModel(10, "Migrate legacy tasks", "v1.0", TaskStatus.DONE),
)

internal val TaskStatus.displayName: String
    get() = when (this) {
        TaskStatus.TODO -> "To-Do"
        TaskStatus.SELECTED -> "Selected"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.READY -> "Ready"
        TaskStatus.DONE -> "Done"
    }
