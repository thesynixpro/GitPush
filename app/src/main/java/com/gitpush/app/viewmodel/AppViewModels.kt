package com.gitpush.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitpush.app.github.ConnectionResult
import com.gitpush.app.github.GitHubRepository
import com.gitpush.app.model.*
import com.gitpush.app.network.NetworkMonitor
import com.gitpush.app.scanner.FolderScanner
import com.gitpush.app.storage.*
import com.gitpush.app.util.ErrorMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Holds the whole push wizard state so screens stay thin. Survives rotation. */
class PushSessionViewModel(
    private val ctx: Context,
    private val github: GitHubRepository,
    private val creds: SecureCredentialStore,
    private val appPrefs: AppPreferences,
    private val db: HistoryDatabase,
    private val network: NetworkMonitor
) : ViewModel() {

    var folderName by mutableStateOf("") private set
    var folderUri by mutableStateOf<Uri?>(null) private set
    val files = mutableStateListOf<ScannedFile>()
    var folderCount by mutableStateOf(0) private set
    val warnings = mutableStateListOf<String>()
    var scanning by mutableStateOf(false) private set
    var scanNote by mutableStateOf("") private set

    var owner by mutableStateOf(creds.owner) private set
    var repo by mutableStateOf(creds.repo) private set
    var branch by mutableStateOf(creds.branch.ifBlank { appPrefs.snapshot().defaultBranch }) private set
    var destPath by mutableStateOf(creds.destPath) private set
    var username by mutableStateOf(creds.username) private set
    var tokenInput by mutableStateOf("") private set  // never persisted in memory longer than needed
    var commitMessage by mutableStateOf(appPrefs.snapshot().defaultCommit) private set
    var policy by mutableStateOf(appPrefs.snapshot().existingPolicy) private set

    var connStatus by mutableStateOf<ConnectionResult?>(null) private set
    var testing by mutableStateOf(false) private set

    var progress by mutableStateOf(PushProgress()) private set
    var pushing by mutableStateOf(false) private set
    var lastSha by mutableStateOf<String?>(null) private set
    var lastError by mutableStateOf<String?>(null) private set
    var lastTechnical by mutableStateOf<String?>(null) private set
    val lastFailedFiles = mutableStateListOf<String>()

    // ASK-policy dialog queue
    var pendingConflict by mutableStateOf<String?>(null) private set
    private var conflictContinuation: CompletableDeferred<ExistingFilePolicy>? = null
    var applyToAll: Boolean = false

    val totalBytes: Long get() = files.filter { it.selected }.sumOf { if (it.size > 0) it.size else 0 }
    val selectedCount: Int get() = files.count { it.selected && it.skipReason == null }
    val totalCount: Int get() = files.size
    val skippedCount: Int get() = files.count { it.skipReason != null }
    val isConfigured: Boolean get() = creds.hasRepoConfig()
    val hasToken: Boolean get() = creds.hasToken()

    fun setField(o: String, r: String, b: String, d: String, u: String, t: String, c: String) {
        owner = o; repo = r; branch = b.ifBlank { "main" }; destPath = d
        username = u; tokenInput = t; commitMessage = c
    }
    fun setPolicy(p: ExistingFilePolicy) { policy = p }

    // ---------- scan ----------
    fun startScan(uri: Uri) {
        folderUri = uri
        files.clear(); warnings.clear()
        scanning = true; lastError = null
        viewModelScope.launch {
            try {
                val res = FolderScanner.scan(ctx, uri) { f, d ->
                    scanNote = "$f files • $d folders"
                }
                folderName = res.folderName
                files.addAll(res.files)
                folderCount = res.folderCount
                warnings.addAll(res.warnings)
                scanNote = ""
            } catch (e: Exception) {
                lastError = ErrorMapper.githubError(e)
                    .takeIf { it != "Something went wrong. Check your connection and try again." }
                    ?: "This file could not be read from the selected folder."
            } finally { scanning = false }
        }
    }

    fun rescan() { folderUri?.let { startScan(it) } }
    fun clearSelection() { files.clear(); folderName = ""; folderUri = null; folderCount = 0 }

    fun toggleFile(id: String) {
        val i = files.indexOfFirst { it.id == id }
        if (i >= 0) files[i] = files[i].copy(selected = !files[i].selected)
    }
    fun setFolderSelected(prefix: String, sel: Boolean) {
        if (prefix.isBlank()) return
        for (i in files.indices) {
            val f = files[i]
            if (f.relativePath == prefix || f.relativePath.startsWith("$prefix/")) {
                files[i] = f.copy(selected = sel)
            }
        }
    }
    fun selectAll(sel: Boolean) {
        for (i in files.indices) files[i] = files[i].copy(selected = sel)
    }

    // ---------- auth / verify ----------
    fun testConnection(onDone: (ConnectionResult) -> Unit = {}) {
        val token = tokenInput.ifBlank { creds.getToken() }
        viewModelScope.launch {
            testing = true
            try {
                val r = github.testConnection(token)
                connStatus = r
                onDone(r)
            } finally { testing = false }
        }
    }

    fun verifyRepo(onDone: (ConnectionResult) -> Unit = {}) {
        val token = tokenInput.ifBlank { creds.getToken() }
        viewModelScope.launch {
            testing = true
            try {
                val r = github.verifyRepo(token, owner.trim(), repo.trim(), branch.trim().ifBlank { "main" })
                connStatus = r
                onDone(r)
            } finally { testing = false }
        }
    }

    fun saveConfiguration(): Boolean {
        if (owner.isBlank() || repo.isBlank()) return false
        val token = tokenInput.trim()
        if (token.isNotEmpty()) creds.saveToken(token)
        if (!creds.hasToken()) return false
        creds.username = username.trim()
        creds.owner = owner.trim()
        creds.repo = repo.trim()
        creds.branch = branch.trim().ifBlank { "main" }
        creds.destPath = destPath.trim().trim('/')
        appPrefs.setDefaultBranch(creds.branch)
        appPrefs.setDefaultCommit(commitMessage)
        appPrefs.setPolicy(policy)
        tokenInput = "" // drop from memory ASAP
        return true
    }

    // ---------- push ----------
    private var pushJob: Job? = null
    @Volatile private var cancelFlag = false
    fun cancelPush() { cancelFlag = true; pushJob?.cancel() }

    fun startPush(onDecisionNeeded: Boolean = true) {
        if (pushing) return
        lastError = null; lastTechnical = null; lastFailedFiles.clear(); lastSha = null
        if (!network.isOnline()) {
            lastError = ErrorMapper.NO_INTERNET
            return
        }
        val token = creds.getToken()
        if (token.isBlank()) {
            lastError = ErrorMapper.AUTH_FAILED
            return
        }
        val cfg = com.gitpush.app.model.RepoConfig(
            username = creds.username, owner = owner.trim(), repo = repo.trim(),
            branch = branch.trim().ifBlank { "main" },
            destPath = destPath.trim().trim('/'),
            commitMessage = commitMessage
        )
        val req = PushRequest(cfg, files.toList(), commitMessage, policy)
        pushing = true; cancelFlag = false
        progress = PushProgress(total = selectedCount)
        val t0 = System.currentTimeMillis()
        pushJob = viewModelScope.launch {
            val outcome = github.push(
                token, req, appPrefs.snapshot().retryCount,
                onProgress = { progress = it },
                shouldCancel = { cancelFlag },
                onNeedDecision = { path ->
                    if (!onDecisionNeeded) return@push ExistingFilePolicy.REPLACE
                    pendingConflict = path
                    conflictContinuation = CompletableDeferred()
                    try { conflictContinuation!!.await() }
                    finally { pendingConflict = null }
                }
            )
            pushing = false
            val elapsed = System.currentTimeMillis() - t0
            when (outcome) {
                is PushOutcome.Success -> {
                    lastSha = outcome.commitSha
                    saveHistory(cfg, "SUCCESS", outcome.commitSha)
                }
                is PushOutcome.Partial -> {
                    lastSha = outcome.commitSha
                    lastFailedFiles.addAll(outcome.failed)
                    lastError = "Uploaded ${outcome.uploaded} file(s), ${outcome.failed.size} failed."
                    saveHistory(cfg, "PARTIAL", outcome.commitSha)
                }
                is PushOutcome.Failed -> {
                    lastError = outcome.userMessage
                    lastTechnical = outcome.technical
                    saveHistory(cfg, "FAILED", null)
                }
                PushOutcome.Cancelled -> {
                    lastError = "Upload cancelled."
                    saveHistory(cfg, "CANCELLED", null)
                }
            }
        }
    }

    fun resolveConflict(replace: Boolean, forAll: Boolean) {
        if (forAll) policy = if (replace) ExistingFilePolicy.REPLACE else ExistingFilePolicy.SKIP
        conflictContinuation?.complete(
            if (replace) ExistingFilePolicy.REPLACE else ExistingFilePolicy.SKIP
        )
        conflictContinuation = null
    }

    fun resetAfterSuccess() {
        clearSelection()
        lastSha = null; lastError = null
        progress = PushProgress()
    }

    private fun saveHistory(cfg: com.gitpush.app.model.RepoConfig, status: String, sha: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                db.historyDao().insert(
                    HistoryEntity(
                        repoFullName = cfg.fullName,
                        branch = cfg.branch,
                        folderName = folderName.ifBlank { "files" },
                        fileCount = selectedCount,
                        folderCount = folderCount,
                        totalBytes = totalBytes,
                        timestampMs = System.currentTimeMillis(),
                        status = status,
                        commitSha = sha
                    )
                )
            }
        }
    }
}

class SettingsViewModel(
    private val ctx: Context,
    val creds: SecureCredentialStore,
    val appPrefs: AppPreferences,
    private val github: GitHubRepository,
    private val db: HistoryDatabase
) : ViewModel() {
    val history: Flow<List<HistoryEntity>> = db.historyDao().observe()
    fun clearHistory() { /* called from scope below */ }
    suspend fun clearHistoryNow() = db.historyDao().clear()
    fun connectionSummary(): String =
        if (creds.hasRepoConfig()) "${creds.owner}/${creds.repo} • ${creds.branch}"
        else "Not configured"
}
