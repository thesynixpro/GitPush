package com.aprax.gitpush.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aprax.gitpush.storage.HistoryEntity
import com.aprax.gitpush.ui.components.GlassCard
import com.aprax.gitpush.ui.theme.accentFor
import com.aprax.gitpush.util.FormatUtils
import com.aprax.gitpush.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(vm: SettingsViewModel, accentName: String) {
    val accent = accentFor(accentName)
    val scope = rememberCoroutineScope()
    val items by vm.history.collectAsState(initial = emptyList())
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Push history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { confirmClear = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear history")
            }
        }
        Text("Stored only on this device. Never uploaded.",
            style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
            GlassCard(accent = accent) { Text("No pushes yet. Your history will appear here.") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { h: HistoryEntity ->
                    GlassCard(accent = accent) {
                        Text(h.repoFullName, fontWeight = FontWeight.Bold)
                        Text("${h.branch} • ${h.folderName} • ${h.fileCount} files • ${FormatUtils.bytes(h.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(FormatUtils.date(h.timestampMs), style = MaterialTheme.typography.labelSmall)
                            Text(
                                h.status, style = MaterialTheme.typography.labelSmall,
                                color = if (h.status == "SUCCESS") MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        h.commitSha?.let {
                            Text("Commit: ${it.take(7)}…", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(70.dp))
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { vm.clearHistoryNow() }
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
