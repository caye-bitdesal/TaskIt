package com.bitdesal.taskit.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitdesal.taskit.domain.TaskStatus

@Composable
fun BoardColumn(
    status: TaskStatus,
    tasks: List<BoardTaskUiModel>,
    modifier: Modifier = Modifier,
    onTaskClick: (BoardTaskUiModel) -> Unit = {},
) {
    val listState = rememberLazyListState()
    
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = status.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = tasks.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (tasks.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "No tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(items = tasks, key = { it.id }) { task ->
                    TaskCard(
                        title = task.title,
                        releaseName = task.releaseName,
                        onClick = { onTaskClick(task) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BoardScreenPreviewFull() {
    MaterialTheme {
        BoardColumn(
            status = TaskStatus.IN_PROGRESS,
            tasks = listOf(
                BoardTaskUiModel(
                    id = 1L,
                    title = "Task 1",
                    releaseName = "Version 1.0.0",
                    status = TaskStatus.IN_PROGRESS
                ),
                BoardTaskUiModel(
                    id = 2L,
                    title = "Task 2",
                    releaseName = "Version 1.0.0",
                    status = TaskStatus.IN_PROGRESS
                ),
                BoardTaskUiModel(
                    id = 3L,
                    title = "Task 3",
                    releaseName = "Version 1.0.0",
                    status = TaskStatus.IN_PROGRESS
                )
            )
        )
    }
}

@Preview
@Composable
private fun BoardScreenPreviewEmpty() {
    MaterialTheme {
        BoardColumn(
            status = TaskStatus.IN_PROGRESS,
            tasks = emptyList()
        )
    }
}