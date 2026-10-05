package com.gitpush.app.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed encrypted storage for the GitHub PAT + account fields.
 * NEVER logs the token. Token is only ever sent to https://api.github.com.
 */
class SecureCredentialStore(context: Context) {

    private val appCtx = context.applicationContext

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(appCtx, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            appCtx,
            "gitpush_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var username: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_USER, v).apply()

    var owner: String
        get() = prefs.getString(KEY_OWNER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_OWNER, v).apply()

    var repo: String
        get() = prefs.getString(KEY_REPO, "") ?: ""
        set(v) = prefs.edit().putString(KEY_REPO, v).apply()

    var branch: String
        get() = prefs.getString(KEY_BRANCH, "main") ?: "main"
        set(v) = prefs.edit().putString(KEY_BRANCH, v).apply()

    var destPath: String
        get() = prefs.getString(KEY_DEST, "") ?: ""
        set(v) = prefs.edit().putString(KEY_DEST, v).apply()

    /** Write token (encrypted at rest via Keystore). */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    /** Read token. Caller must never log or display it. */
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun hasToken(): Boolean = getToken().isNotBlank()

    /** True when minimal config is present. */
    fun hasRepoConfig(): Boolean =
        owner.isNotBlank() && repo.isNotBlank() && branch.isNotBlank() && hasToken()

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER)
            .remove(KEY_OWNER)
            .remove(KEY_REPO)
            .remove(KEY_BRANCH)
            .remove(KEY_DEST)
            .apply()
    }

    fun clearTokenOnly() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "github_pat"
        private const val KEY_USER = "github_username"
        private const val KEY_OWNER = "repo_owner"
        private const val KEY_REPO = "repo_name"
        private const val KEY_BRANCH = "repo_branch"
        private const val KEY_DEST = "repo_dest_path"
    }
}
