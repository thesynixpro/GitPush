package com.gitpush.app.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed encrypted storage for the GitHub PAT + account fields.
 * NEVER logs the token. Token is only ever sent to https://api.github.com.
 *
 * Fail-safe: if the Android Keystore / encrypted prefs cannot be initialised
 * on this device, the store reports [isAvailable] = false and every accessor
 * degrades to safe defaults instead of throwing. This guarantees the app
 * always opens; the UI explains that GitHub login is unavailable.
 */
class SecureCredentialStore(context: Context) {

    private val appCtx = context.applicationContext

    private var encrypted: SharedPreferences? = null

    /** Human-readable reason when [isAvailable] is false. Never contains secrets. */
    var unavailableReason: String? = null
        private set

    val isAvailable: Boolean get() = encrypted != null

    init {
        encrypted = try {
            val masterKey = MasterKey.Builder(appCtx, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appCtx,
                "gitpush_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            unavailableReason = e::class.java.simpleName
            null
        }
    }

    private fun get(key: String, def: String): String {
        return try {
            encrypted?.getString(key, def) ?: def
        } catch (_: Exception) {
            def
        }
    }

    private fun put(key: String, value: String): Boolean {
        return try {
            val p = encrypted ?: return false
            p.edit().putString(key, value).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    var username: String
        get() = get(KEY_USER, "")
        set(v) { put(KEY_USER, v) }

    var owner: String
        get() = get(KEY_OWNER, "")
        set(v) { put(KEY_OWNER, v) }

    var repo: String
        get() = get(KEY_REPO, "")
        set(v) { put(KEY_REPO, v) }

    var branch: String
        get() = get(KEY_BRANCH, "main").ifBlank { "main" }
        set(v) { put(KEY_BRANCH, v) }

    var destPath: String
        get() = get(KEY_DEST, "")
        set(v) { put(KEY_DEST, v) }

    /** Write token (encrypted at rest via Keystore). Returns false when storage is unavailable. */
    fun saveToken(token: String): Boolean = put(KEY_TOKEN, token.trim())

    /** Read token. Caller must never log or display it. */
    fun getToken(): String = get(KEY_TOKEN, "")

    fun hasToken(): Boolean = isAvailable && getToken().isNotBlank()

    /** True when minimal config is present. */
    fun hasRepoConfig(): Boolean =
        isAvailable && owner.isNotBlank() && repo.isNotBlank() &&
            branch.isNotBlank() && hasToken()

    fun clearCredentials() {
        try {
            encrypted?.edit()
                ?.remove(KEY_TOKEN)
                ?.remove(KEY_USER)
                ?.remove(KEY_OWNER)
                ?.remove(KEY_REPO)
                ?.remove(KEY_BRANCH)
                ?.remove(KEY_DEST)
                ?.apply()
        } catch (_: Exception) { /* best effort */ }
    }

    fun clearTokenOnly() {
        try {
            encrypted?.edit()?.remove(KEY_TOKEN)?.apply()
        } catch (_: Exception) { /* best effort */ }
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
