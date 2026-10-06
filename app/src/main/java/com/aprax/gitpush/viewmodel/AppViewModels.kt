package com.aprax.gitpush.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aprax.gitpush.github.ConnectionResult
import com.aprax.gitpush.github.GitHubRepository
import com.aprax.gitpush.model.*
import com.aprax.gitpush.network.NetworkMonitor
import com.aprax.gitpush.scanner.FolderScanner
import com.aprax.gitpush.storage.*
import com.aprax.gitpush.util.ErrorMapper
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

    // Compose state with explicit backing fields (no `by ... private set` delegation).
    private val _folderName = mutableStateOf("")
    var folderName: String
        get() = _folderName.value
        private set(value) { _folderName.value = value }

    private val _folderUri = mutableStateOf<Uri?>(null)
    var folderUri: Uri?
        get() = _folderUri.value
        private set(value) { _folderUri.value = value }

    val files = mutableStateListOf<ScannedFile>()

    private val _folderCount = mutableStateOf(0)
    var folderCount: Int
        get() = _folderCount.value
        private set(value) { _folderCount.value = value }

    val warnings = mutableStateListOf<String>()

    private val _scanning = mutableStateOf(false)
    var scanning: Boolean
        get() = _scanning.value
        private set(value) { _scanning.value = value }

    private val _scanNote = mutableStateOf("")
    var scanNote: String
        get() = _scanNote.value
        private set(value) { _scanNote.value = value }

    private val _owner = mutableStateOf(creds.owner)
    var owner: String
        get() = _owner.value
        private set(value) { _owner.value = value }

    private val _repo = mutableStateOf(creds.repo)
    var repo: String
        get() = _repo.value
        private set(value) { _repo.value = value }

    private val _branch = mutableStateOf(creds.branch.ifBlank { appPrefs.snapshot().defaultBranch })
    var branch: String
        get() = _branch.value
        private set(value) { _branch.value = value }

    private val _destPath = mutableStateOf(creds.destPath)
    var destPath: String
        get() = _destPath.value
        private set(value) { _destPath.value = value }

    private val _username = mutableStateOf(creds.username)
    var username: String
        get() = _username.value
        private set(value) { _username.value = value }

    private val _tokenInput = mutableStateOf("")
    var tokenInput: String // never persisted in memory longer than needed
        get() = _tokenInput.value
        private set(value) { _tokenInput.value = value }

    private val _commitMessage = mutableStateOf(appPrefs.snapshot().defaultCommit)
    var commitMessage: String
        get() = _commitMessage.value
        private set(value) { _commitMessage.value = value }

    private val _policy = mutableStateOf(appPrefs.snapshot().existingPolicy)
    var policy: ExistingFilePolicy
        get() = _policy.value
        private set(value) { _policy.value = value }

    private val _connStatus = mutableStateOf<ConnectionResult?>(null)
    var connStatus: ConnectionResult?
        get() = _connStatus.value
        private set(value) { _connStatus.value = value }

    private val _testing = mutableStateOf(false)
    var testing: Boolean
        get() = _testing.value
        private set(value) { _testing.value = value }

    private val _progress = mutableStateOf(PushProgress())
    var progress: PushProgress
        get() = _progress.value
        private set(value) { _progress.value = value }

    private val _pushing = mutableStateOf(false)
    var pushing: Boolean
        get() = _pushing.value
        private set(value) { _pushing.value = value }

    private val _lastSha = mutableStateOf<String?>(null)
    var lastSha: String?
        get() = _lastSha.value
        private set(value) { _lastSha.value = value }

    private val _lastError = mutableStateOf<String?>(null)
    var lastError: String?
        get() = _lastError.value
        private set(value) { _lastError.value = value }

    private val _lastTechnical = mutableStateOf<String?>(null)
    var lastTechnical: String?
        get() = _lastTechnical.value
        private set(value) { _lastTechnical.value = value }

    val lastFailedFiles = mutableStateListOf<String>()

    // ASK-policy dialog queue
    private val _pendingConflict = mutableStateOf<String?>(null)
    var pendingConflict: String?
        get() = _pendingConflict.value
        private set(value) { _pendingConflict.value = value }
    private var conflictContinuation: CompletableDeferred<ExistingFilePolicy>? = null
    var applyToAll: Boolean = false

    val totalBytes: Long get() = files.filter { it.selected }.sumOf { if (it.size > 0) it.size else 0 }
    val selectedCount: Int get() = files.count { it.selected && it.skipReason == null }
    val totalCount: Int get() = files.size
    val skippedCount: Int get() = files.count { it.skipReason != null }
    val isConfigured: Boolean get() = creds.hasRepoConfig()
    val hasToken: Boolean get() = creds.hasToken()
    val storageAvailable: Boolean get() = creds.isAvailable
    val storageError: String? get() = creds.unavailableReason

    fun setField(o: String, r: String, b: String, d: String, u: String, t: String, c: String) {
        owner = o; repo = r; branch = b.ifBlank { "main" }; destPath = d
        username = u; tokenInput = t; commitMessage = c
    }
    fun updatePolicy(p: ExistingFilePolicy) { policy = p }

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
        if (!creds.isAvailable) return false
        if (owner.isBlank() || repo.isBlank()) return false
        val token = tokenInput.trim()
        if (token.isNotEmpty() && !creds.saveToken(token)) return false
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
        val cfg = com.aprax.gitpush.model.RepoConfig(
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

    private fun saveHistory(cfg: com.aprax.gitpush.model.RepoConfig, status: String, sha: String?) {
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
