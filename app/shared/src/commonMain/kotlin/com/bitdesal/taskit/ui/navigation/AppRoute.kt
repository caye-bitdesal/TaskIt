package com.bitdesal.taskit.ui.navigation

sealed interface AppRoute {
    data object Board : AppRoute

    data object ReleaseList : AppRoute

    data object ReleaseEditor : AppRoute

    data object TaskEditor : AppRoute
}
