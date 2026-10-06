package com.aprax.gitpush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.*
import com.aprax.gitpush.ui.navigation.Routes
import com.aprax.gitpush.ui.screens.*
import com.aprax.gitpush.ui.theme.GitPushTheme
import com.aprax.gitpush.util.CrashReporter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as GitPushApplication
            val session = remember { app.session }
            val settings = remember { app.settings }
            val snap by settings.appPrefs.state.collectAsState()
            var showSplash by remember { mutableStateOf(true) }
            var crashFile by remember {
                mutableStateOf(runCatching { CrashReporter.pendingReport(this@MainActivity) }.getOrNull())
            }

            GitPushTheme(themeMode = snap.themeMode) {
                if (crashFile != null) {
                    // Previous run died: show its trace with copy button instead of normal UI.
                    val text = remember(crashFile) {
                        CrashReporter.readReport(crashFile!!)
                    }
                    CrashReportScreen(
                        reportText = text,
                        accentName = snap.accent,
                        onCopy = {},
                        onContinue = {
                            runCatching { crashFile!!.delete() }
                            CrashReporter.clearReports(this@MainActivity)
                            crashFile = null
                        }
                    )
                } else if (showSplash) {
                    SplashScreen { showSplash = false }
                } else {
                    val nav = rememberNavController()
                    val backStack by nav.currentBackStackEntryAsState()
                    val route = backStack?.destination?.route
                    val showBottom = route in
                        listOf(Routes.HOME, Routes.REVIEW, Routes.HISTORY, Routes.SETTINGS)

                    Scaffold(
                        bottomBar = {
                            if (showBottom) {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = route == Routes.HOME,
                                        onClick = { nav.navigate(Routes.HOME) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                        label = { Text("Home") }
                                    )
                                    NavigationBarItem(
                                        selected = route == Routes.REVIEW,
                                        onClick = { nav.navigate(Routes.REVIEW) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Filled.CloudUpload, contentDescription = "Push") },
                                        label = { Text("Push") }
                                    )
                                    NavigationBarItem(
                                        selected = route == Routes.HISTORY,
                                        onClick = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                                        label = { Text("History") }
                                    )
                                    NavigationBarItem(
                                        selected = route == Routes.SETTINGS,
                                        onClick = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        }
                    ) { pad ->
                        Box(Modifier.padding(pad)) {
                            NavHost(nav, startDestination = Routes.HOME) {
                                composable(Routes.HOME) {
                                    HomeScreen(
                                        vm = session, accentName = snap.accent,
                                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                                        onReview = { nav.navigate(Routes.REVIEW) },
                                        onHistory = { nav.navigate(Routes.HISTORY) }
                                    )
                                }
                                composable(Routes.REVIEW) {
                                    ReviewScreen(
                                        vm = session, accentName = snap.accent,
                                        onContinue = {
                                            nav.navigate(
                                                if (session.isConfigured) Routes.CONFIRM else Routes.REPO
                                            )
                                        },
                                        onCancel = { session.clearSelection(); nav.navigate(Routes.HOME) }
                                    )
                                }
                                composable(Routes.REPO) {
                                    RepoConfigScreen(
                                        vm = session, accentName = snap.accent,
                                        onNext = {
                                            if (snap.confirmBeforePush) {
                                                nav.navigate(Routes.CONFIRM)
                                            } else {
                                                session.startPush()
                                                nav.navigate(Routes.PROGRESS)
                                            }
                                        },
                                        onBack = { nav.popBackStack() }
                                    )
                                }
                                composable(Routes.CONFIRM) {
                                    ConfirmScreen(
                                        vm = session, accentName = snap.accent,
                                        onPush = {
                                            session.startPush()
                                            nav.navigate(Routes.PROGRESS)
                                        },
                                        onBack = { nav.popBackStack() }
                                    )
                                }
                                composable(Routes.PROGRESS) {
                                    ProgressScreen(
                                        vm = session, accentName = snap.accent,
                                        onDone = { nav.navigate(Routes.SUCCESS) { popUpTo(Routes.HOME) } },
                                        onCancelPush = {
                                            session.cancelPush()
                                            nav.popBackStack()
                                        }
                                    )
                                }
                                composable(Routes.SUCCESS) {
                                    SuccessScreen(
                                        vm = session, accentName = snap.accent,
                                        onViewRepo = {},
                                        onPushAnother = {
                                            nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
                                        },
                                        onDone = {
                                            nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
                                        }
                                    )
                                }
                                composable(Routes.HISTORY) {
                                    HistoryScreen(vm = settings, accentName = snap.accent)
                                }
                                composable(Routes.SETTINGS) {
                                    SettingsScreen(
                                        settings = settings, session = session,
                                        accentName = snap.accent,
                                        onAccent = { settings.appPrefs.setAccent(it) },
                                        onTheme = { settings.appPrefs.setTheme(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
