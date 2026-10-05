package com.gitpush.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitpush.app.ui.components.*
import com.gitpush.app.ui.theme.accentFor
import com.gitpush.app.util.FormatUtils
import com.gitpush.app.viewmodel.PushSessionViewModel

@Composable
fun ConfirmScreen(vm: PushSessionViewModel, accentName: String, onPush: () -> Unit, onBack: () -> Unit) {
    val accent = accentFor(accentName)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Confirm push", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        GlassCard(accent = accent) {
            KV("Repository", "${vm.owner}/${vm.repo}")
            KV("Branch", vm.branch)
            KV("Destination", if (vm.destPath.isBlank()) "/" else "/${vm.destPath}")
            KV("Files", "${vm.selectedCount}")
            KV("Folders", "${vm.folderCount}")
            KV("Total size", FormatUtils.bytes(vm.totalBytes))
            Spacer(Modifier.height(6.dp))
            Text("Commit: “${vm.commitMessage.ifBlank { "Upload files from Android" }}”",
                style = MaterialTheme.typography.bodySmall)
        }
        Text("GitHub does not store empty directories — empty folders are ignored.",
            style = MaterialTheme.typography.labelSmall)
        GradientButton("Push to GitHub", onClick = onPush, accent = accent)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
private fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(v, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProgressScreen(vm: PushSessionViewModel, accentName: String, onDone: () -> Unit, onCancelPush: () -> Unit) {
    val accent = accentFor(accentName)
    val p = vm.progress
    val frac = if (p.total > 0) (p.done + p.skipped).toFloat() / p.total else 0f
    val elapsed = System.currentTimeMillis() - p.startedAtMs
    val bps = if (elapsed > 0) p.bytesUploaded * 1000.0 / elapsed else 0.0

    // Conflict dialog for ASK policy
    vm.pendingConflict?.let { path ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("File already exists") },
            text = { Text("$path already exists in the repository.") },
            confirmButton = {
                TextButton(onClick = { vm.resolveConflict(true, false) }) { Text("Replace") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.resolveConflict(false, false) }) { Text("Skip") }
                    TextButton(onClick = { vm.resolveConflict(false, true) }) { Text("Skip all") }
                }
            }
        )
    }

    LaunchedEffect(vm.pushing, vm.lastError, vm.lastSha) {
        if (!vm.pushing && (vm.lastSha != null || vm.lastError != null)) {
            if (vm.lastSha != null) onDone()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Uploading…", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${p.done} / ${p.total} files • ${p.percent}%",
            style = MaterialTheme.typography.titleMedium)
        NeonProgress(frac, accent)
        GlassCard(accent = accent) {
            KV("Current file", p.currentFile.ifBlank { "—" })
            KV("Current folder", p.currentFolder.ifBlank { "—" })
            KV("Uploaded", "${p.done}") ; KV("Failed", "${p.failed}") ; KV("Remaining", "${p.remaining}")
            KV("Speed", FormatUtils.speed(p.bytesUploaded, elapsed))
            KV("ETA", FormatUtils.eta(p.totalBytes - p.bytesUploaded, bps))
        }
        if (vm.pushing) {
            CircularProgressIndicator(color = accent)
            OutlinedButton(onClick = onCancelPush, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
        vm.lastError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            vm.lastTechnical?.let { t ->
                Text("Details: $t", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.startPush() }) { Text("Retry") }
                OutlinedButton(onClick = onCancelPush) { Text("Cancel") }
            }
        }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
fun SuccessScreen(
    vm: PushSessionViewModel,
    accentName: String,
    onViewRepo: () -> Unit,
    onPushAnother: () -> Unit,
    onDone: () -> Unit
) {
    val accent = accentFor(accentName)
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Filled.CheckCircle, null, tint = accent, modifier = Modifier.size(84.dp))
        Text("Push Complete", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        GlassCard(accent = accent) {
            Text("✓ ${vm.progress.done} files uploaded")
            Text("✓ ${vm.folderCount} folders processed")
            Text("✓ Repository updated")
            Spacer(Modifier.height(8.dp))
            Text("Commit: ${vm.lastSha?.take(7) ?: "—"}…", style = MaterialTheme.typography.bodySmall)
            Text("Repository: ${vm.owner}/${vm.repo}", style = MaterialTheme.typography.bodySmall)
            Text("Branch: ${vm.branch}", style = MaterialTheme.typography.bodySmall)
            if (vm.lastFailedFiles.isNotEmpty()) {
                Text("${vm.lastFailedFiles.size} file(s) failed — see history.",
                    color = MaterialTheme.colorScheme.error)
            }
        }
        GradientButton("View Repository", onClick = {
            val url = "https://github.com/${vm.owner}/${vm.repo}/tree/${vm.branch}"
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            onViewRepo()
        }, accent = accent)
        OutlinedButton(onClick = { vm.resetAfterSuccess(); onPushAnother() }, modifier = Modifier.fillMaxWidth()) {
            Text("Push Another Folder")
        }
        TextButton(onClick = { vm.resetAfterSuccess(); onDone() }) { Text("Done") }
        Spacer(Modifier.height(70.dp))
    }
}
