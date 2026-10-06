package com.gitpush.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitpush.app.ui.components.*
import com.gitpush.app.ui.theme.NeonCyan
import com.gitpush.app.ui.theme.accentFor
import com.gitpush.app.util.FormatUtils
import com.gitpush.app.viewmodel.PushSessionViewModel

@Composable
fun HomeScreen(
    vm: PushSessionViewModel,
    accentName: String,
    onOpenSettings: () -> Unit,
    onReview: () -> Unit,
    onHistory: () -> Unit
) {
    val accent = accentFor(accentName)
    val ctx = LocalContext.current
    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist read permission for later upload.
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.startScan(uri)
            onReview()
        }
    }

    val configured = vm.isConfigured
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("GitPush", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (configured) "${vm.owner}/${vm.repo} • ${vm.branch}"
                        else "Not connected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            Row {
                StatusBadge(
                    ok = if (configured) true else null,
                    text = if (configured) "Linked" else "Setup needed"
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }

        if (!vm.storageAvailable) {
            GlassCard(accent = MaterialTheme.colorScheme.error) {
                Text(
                    "Secure storage is unavailable on this device" +
                        (vm.storageError?.let { " ($it)" } ?: "") +
                        ". GitHub login is disabled, but you can still browse and review folders.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Hero card
        GlassCard(accent = accent) {
            Text("Push Files to GitHub", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Select a folder from your device and push its contents directly to your GitHub repository.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(14.dp))
            GradientButton("Select Folder", onClick = { pickFolder.launch(null) }, accent = accent)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your files are sent directly from your device to GitHub.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        }

        // Quick stats / preview after a scan
        if (vm.totalCount > 0) {
            GlassCard(accent = accent) {
                SectionLabel("Selected folder")
                Text(vm.folderName, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${vm.totalCount} files • ${vm.folderCount} folders • ${FormatUtils.bytes(vm.totalBytes)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    FormatUtils.treePreview(vm.files.take(30).map { it.relativePath }),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(10.dp))
                GradientButton("Review Files", onClick = onReview, accent = accent)
            }
        } else {
            GlassCard(accent = accent) {
                SectionLabel("How it works")
                listOf(
                    "1. Select Folder to Push",
                    "2. Review files",
                    "3. Configure repository & token",
                    "4. Confirm & push"
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.History, null); Spacer(Modifier.width(6.dp)); Text("View push history")
                }
            }
        }

        if (vm.scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accent)
            Text("Scanning… ${vm.scanNote}", style = MaterialTheme.typography.labelSmall)
        }
        vm.lastError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(70.dp))
    }
}
