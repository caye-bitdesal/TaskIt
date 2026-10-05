package com.bitdesal.taskit.ui

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
    IconButton(
        onClick = onToggle,
        modifier = modifier.semantics { contentDescription = label },
    ) {
        Text(
            text = if (isDarkTheme) "☀" else "☾",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
