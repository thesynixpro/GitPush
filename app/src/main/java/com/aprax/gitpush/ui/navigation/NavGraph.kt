package com.aprax.gitpush.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val REVIEW = "review"
    const val REPO = "repo"
    const val CONFIRM = "confirm"
    const val PROGRESS = "progress"
    const val SUCCESS = "success"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

sealed class BottomTab(val route: String, val label: String) {
    data object Home : BottomTab(Routes.HOME, "Home")
    data object Push : BottomTab(Routes.REVIEW, "Push")
    data object History : BottomTab(Routes.HISTORY, "History")
    data object Settings : BottomTab(Routes.SETTINGS, "Settings")
    companion object { val all = listOf(Home, Push, History, Settings) }
}
