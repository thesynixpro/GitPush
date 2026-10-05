package com.gitpush.app

import android.app.Application
import com.gitpush.app.github.GitHubRepository
import com.gitpush.app.network.NetworkMonitor
import com.gitpush.app.storage.AppPreferences
import com.gitpush.app.storage.HistoryDatabase
import com.gitpush.app.storage.SecureCredentialStore
import com.gitpush.app.viewmodel.PushSessionViewModel
import com.gitpush.app.viewmodel.SettingsViewModel

/** Manual DI container — no external backend, everything on-device. */
class GitPushApplication : Application() {
    lateinit var creds: SecureCredentialStore private set
    lateinit var prefs: AppPreferences private set
    lateinit var github: GitHubRepository private set
    lateinit var network: NetworkMonitor private set
    lateinit var db: HistoryDatabase private set

    lateinit var session: PushSessionViewModel private set
    lateinit var settings: SettingsViewModel private set

    override fun onCreate() {
        super.onCreate()
        creds = SecureCredentialStore(this)
        prefs = AppPreferences(this)
        github = GitHubRepository(this)
        network = NetworkMonitor(this)
        db = HistoryDatabase.get(this)
        session = PushSessionViewModel(this, github, creds, prefs, db, network)
        settings = SettingsViewModel(this, creds, prefs, github, db)
    }
}
