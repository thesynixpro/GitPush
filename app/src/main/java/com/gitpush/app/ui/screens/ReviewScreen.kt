package com.gitpush.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitpush.app.ui.components.*
import com.gitpush.app.ui.theme.accentFor
import com.gitpush.app.util.FormatUtils
import com.gitpush.app.viewmodel.PushSessionViewModel

@Composable
fun ReviewScreen(
    vm: PushSessionViewModel,
    accentName: String,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    val accent = accentFor(accentName)
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(setOf<String>()) }

    val filtered = remember(vm.files.toList(), query) {
        if (query.isBlank()) vm.files.toList()
        else vm.files.filter { it.relativePath.contains(query, true) }
    }
    // Group by top-level folder for expand/collapse.
    val groups = remember(filtered) {
        filtered.groupBy { it.relativePath.substringBefore('/', "__root__") }.toSortedMap()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(vm.folderName.ifBlank { "Review files" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${vm.selectedCount}/${vm.totalCount} files • ${vm.folderCount} folders • ${FormatUtils.bytes(vm.totalBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
            IconButton(onClick = { vm.rescan() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh folder contents")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (vm.scanning) {
            NeonProgress(0.3f, accent); Spacer(Modifier.height(4.dp))
            Text("Scanning… ${vm.scanNote}", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search files…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.selectAll(true) }, modifier = Modifier.weight(1f)) { Text("Select all") }
            OutlinedButton(onClick = { vm.selectAll(false) }, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
        Spacer(Modifier.height(8.dp))

        vm.warnings.forEach { w ->
            Text("⚠ $w", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            groups.forEach { (folder, list) ->
                val isRoot = folder == "__root__"
                val label = if (isRoot) "Files" else "$folder/"
                val allSel = list.all { it.selected }
                item(key = "hdr-$folder") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                expanded = if (expanded.contains(folder)) expanded - folder else expanded + folder
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (expanded.contains(folder)) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                            contentDescription = if (expanded.contains(folder)) "Collapse $label" else "Expand $label"
                        )
                        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${list.count { it.selected }}/${list.size}", style = MaterialTheme.typography.labelSmall)
                        Checkbox(
                            checked = allSel,
                            onCheckedChange = { checked ->
                                if (isRoot) {
                                    list.forEach { f ->
                                        val cur = vm.files.firstOrNull { it.id == f.id }?.selected
                                        if (cur != null && cur != checked) vm.toggleFile(f.id)
                                    }
                                } else {
                                    vm.setFolderSelected(folder, checked)
                                }
                            }
                        )
                    }
                    Divider()
                }
                if (expanded.contains(folder) || query.isNotBlank()) {
                    items(list, key = { it.id }) { f ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.InsertDriveFile, null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.relativePath, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    FormatUtils.bytes(f.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                            Checkbox(checked = f.selected, onCheckedChange = { vm.toggleFile(f.id) })
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        GradientButton(
            "Continue to GitHub settings (${vm.selectedCount})",
            onClick = onContinue, accent = accent,
            enabled = vm.selectedCount > 0 && !vm.scanning
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        Spacer(Modifier.height(60.dp))
    }
}
