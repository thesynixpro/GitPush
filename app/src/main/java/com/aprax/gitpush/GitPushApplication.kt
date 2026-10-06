package com.aprax.gitpush

import android.app.Application
import com.aprax.gitpush.github.GitHubRepository
import com.aprax.gitpush.network.NetworkMonitor
import com.aprax.gitpush.storage.AppPreferences
import com.aprax.gitpush.storage.HistoryDatabase
import com.aprax.gitpush.storage.SecureCredentialStore
import com.aprax.gitpush.viewmodel.PushSessionViewModel
import com.aprax.gitpush.viewmodel.SettingsViewModel
import com.aprax.gitpush.util.CrashReporter

/** Manual DI container — no external backend, everything on-device. */
class GitPushApplication : Application() {
    lateinit var creds: SecureCredentialStore private set
    lateinit var prefs: AppPreferences private set
    lateinit var github: GitHubRepository private set
    lateinit var network: NetworkMonitor private set
    lateinit var db: HistoryDatabase private set

    lateinit var session: PushSessionViewModel private set
    lateinit var settings: SettingsViewModel private set

    /** Startup must never crash: secure storage degrades gracefully, DB falls back to memory. */
    override fun onCreate() {
        super.onCreate()
        // Install first: any later startup crash is saved to Downloads + files/crashes.
        runCatching { CrashReporter.install(this) }
        creds = SecureCredentialStore(this) // never throws; reports isAvailable instead
        prefs = AppPreferences(this)
        github = GitHubRepository(this)
        network = NetworkMonitor(this)
        db = runCatching { HistoryDatabase.get(this) }.getOrElse {
            // Last-resort in-memory history so the app still works.
            androidx.room.Room.inMemoryDatabaseBuilder(this, HistoryDatabase::class.java).build()
        }
        session = PushSessionViewModel(this, github, creds, prefs, db, network)
        settings = SettingsViewModel(this, creds, prefs, github, db)
    }
}
