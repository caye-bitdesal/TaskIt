package com.bitdesal.taskit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bitdesal.taskit.di.AppGraph
import com.bitdesal.taskit.ui.TaskItScaffold
import com.bitdesal.taskit.ui.navigation.AppRoute
import dev.zacsweers.metro.createGraph

@Composable
@Preview
fun App() {
    val appGraph = remember { createGraph<AppGraph>() }

    var darkThemeOverride by remember { mutableStateOf<Boolean?>(null) }
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = darkThemeOverride ?: systemDark

    MaterialTheme(
        colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        var route by remember { mutableStateOf<AppRoute>(AppRoute.Board) }

        val screenTitle = when (route) {
            AppRoute.Board -> "Board"
            AppRoute.ReleaseList -> "Releases"
            AppRoute.ReleaseEditor -> "Release editor"
            AppRoute.TaskEditor -> "Task editor"
        }

        TaskItScaffold(
            title = screenTitle,
            isDarkTheme = useDarkTheme,
            onNavigateToReleases = { route = AppRoute.ReleaseList },
            onThemeToggle = {
                val effectiveDark = darkThemeOverride ?: systemDark
                darkThemeOverride = !effectiveDark
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                when (route) {
                    AppRoute.Board -> Text("Board")
                    AppRoute.ReleaseList -> Text("Release list")
                    AppRoute.ReleaseEditor -> Text("Release editor")
                    AppRoute.TaskEditor -> Text("Task editor")
                }
            }
        }
    }
}
