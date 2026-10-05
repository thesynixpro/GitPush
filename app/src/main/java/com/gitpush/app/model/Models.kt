package com.gitpush.app.model

import android.net.Uri

/** A single file discovered via SAF. Content is NOT held in RAM — only metadata + Uri. */
data class ScannedFile(
    val id: String,              // relative path, e.g. "assets/logo.png"
    val displayName: String,     // "logo.png"
    val relativePath: String,    // "assets/logo.png"
    val uri: Uri,                // DocumentFile uri for lazy reading
    val size: Long,              // -1 if unknown
    val mimeType: String?,
    var selected: Boolean = true,
    var skipReason: String? = null // non-null => will be skipped
)

data class ScanStats(
    val folderName: String,
    val folderUri: String,
    val fileCount: Int,
    val folderCount: Int,
    val totalBytes: Long,
    val selectableCount: Int,
    val skippedCount: Int
)

data class RepoConfig(
    val username: String = "",
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val destPath: String = "",          // optional/path inside repo, "" = root
    val commitMessage: String = "Upload files from Android"
) {
    val fullName: String get() = "$owner/$repo".trim('/')
    fun normalizedDest(prefix: String = destPath.trim().trim('/')) = prefix
}

enum class ExistingFilePolicy { ASK, REPLACE, SKIP }

data class PushRequest(
    val config: RepoConfig,
    val files: List<ScannedFile>,
    val commitMessage: String,
    val policy: ExistingFilePolicy
)

data class PushProgress(
    val total: Int = 0,
    val done: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val currentFile: String = "",
    val currentFolder: String = "",
    val percent: Int = 0,
    val bytesUploaded: Long = 0L,
    val totalBytes: Long = 0L,
    val startedAtMs: Long = System.currentTimeMillis()
) {
    val remaining: Int get() = (total - done - failed - skipped).coerceAtLeast(0)
}

sealed interface PushOutcome {
    data class Success(val commitSha: String, val uploaded: Int, val skipped: Int) : PushOutcome
    data class Partial(val commitSha: String?, val uploaded: Int, val failed: List<String>) : PushOutcome
    data class Failed(val userMessage: String, val technical: String?) : PushOutcome
    data object Cancelled : PushOutcome
}
