package com.gitpush.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.gitpush.app.model.ExistingFilePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Non-sensitive user preferences. Token lives in SecureCredentialStore (encrypted). */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("gitpush_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<PrefsSnapshot> = _state.asStateFlow()

    fun snapshot(): PrefsSnapshot = PrefsSnapshot(
        defaultBranch = prefs.getString(K_BRANCH, "main") ?: "main",
        defaultCommit = prefs.getString(K_COMMIT, "Upload files from Android")
            ?: "Upload files from Android",
        existingPolicy = runCatching {
            ExistingFilePolicy.valueOf(prefs.getString(K_POLICY, "ASK") ?: "ASK")
        }.getOrDefault(ExistingFilePolicy.ASK),
        confirmBeforePush = prefs.getBoolean(K_CONFIRM, true),
        retryCount = prefs.getInt(K_RETRY, 2),
        themeMode = prefs.getString(K_THEME, "SYSTEM") ?: "SYSTEM",
        accent = prefs.getString(K_ACCENT, "CYAN") ?: "CYAN"
    )

    private fun refresh() { _state.value = snapshot() }

    fun setDefaultBranch(v: String) { prefs.edit().putString(K_BRANCH, v).apply(); refresh() }
    fun setDefaultCommit(v: String) { prefs.edit().putString(K_COMMIT, v).apply(); refresh() }
    fun setPolicy(v: ExistingFilePolicy) { prefs.edit().putString(K_POLICY, v.name).apply(); refresh() }
    fun setConfirm(v: Boolean) { prefs.edit().putBoolean(K_CONFIRM, v).apply(); refresh() }
    fun setRetry(v: Int) { prefs.edit().putInt(K_RETRY, v.coerceIn(0, 5)).apply(); refresh() }
    fun setTheme(v: String) { prefs.edit().putString(K_THEME, v).apply(); refresh() }
    fun setAccent(v: String) { prefs.edit().putString(K_ACCENT, v).apply(); refresh() }

    companion object {
        private const val K_BRANCH = "default_branch"
        private const val K_COMMIT = "default_commit"
        private const val K_POLICY = "existing_policy"
        private const val K_CONFIRM = "confirm_before_push"
        private const val K_RETRY = "retry_count"
        private const val K_THEME = "theme_mode"
        private const val K_ACCENT = "accent"
    }
}

data class PrefsSnapshot(
    val defaultBranch: String = "main",
    val defaultCommit: String = "Upload files from Android",
    val existingPolicy: ExistingFilePolicy = ExistingFilePolicy.ASK,
    val confirmBeforePush: Boolean = true,
    val retryCount: Int = 2,
    val themeMode: String = "SYSTEM",
    val accent: String = "CYAN"
)
