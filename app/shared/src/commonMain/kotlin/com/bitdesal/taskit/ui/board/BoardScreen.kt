package com.bitdesal.taskit.ui.board

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitdesal.taskit.domain.TaskStatus

private val NarrowBoardWidth = 720.dp
private val BoardColumnMinWidth = 260.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    modifier: Modifier = Modifier,
) {
    val tasks = remember { sampleBoardTasks() }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.semantics { contentDescription = "New task" },
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            val narrow = maxWidth < NarrowBoardWidth
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (narrow) {
                            Modifier.horizontalScroll(horizontalScrollState)
                        } else {
                            Modifier
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskStatus.entries.forEach { status ->
                    val columnTasks = tasks.filter { it.status == status }
                    BoardColumn(
                        status = status,
                        tasks = columnTasks,
                        modifier = if (narrow) {
                            Modifier
                                .width(BoardColumnMinWidth)
                                .fillMaxHeight()
                        } else {
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BoardScreenPreview() {
    MaterialTheme {
        BoardScreen()
    }
}
