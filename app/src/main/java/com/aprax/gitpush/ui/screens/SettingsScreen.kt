package com.aprax.gitpush.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aprax.gitpush.model.ExistingFilePolicy
import com.aprax.gitpush.ui.components.*
import com.aprax.gitpush.ui.theme.accentFor
import com.aprax.gitpush.viewmodel.PushSessionViewModel
import com.aprax.gitpush.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsViewModel,
    session: PushSessionViewModel,
    accentName: String,
    onAccent: (String) -> Unit,
    onTheme: (String) -> Unit
) {
    val accent = accentFor(accentName)
    val scope = rememberCoroutineScope()
    val snap by settings.appPrefs.state.collectAsState()
    var showClearCreds by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        GlassCard(accent = accent) {
            SectionLabel("GitHub account")
            Text(settings.connectionSummary(), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Token: ${if (settings.creds.hasToken()) "•••• saved (encrypted)" else "not set"}",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showClearCreds = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear saved credentials")
            }
        }

        GlassCard(accent = accent) {
            SectionLabel("Push settings")
            OutlinedTextField(snap.defaultBranch, { settings.appPrefs.setDefaultBranch(it) },
                Modifier.fillMaxWidth(), label = { Text("Default branch") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(snap.defaultCommit, { settings.appPrefs.setDefaultCommit(it) },
                Modifier.fillMaxWidth(), label = { Text("Default commit message") })
            Spacer(Modifier.height(8.dp))
            Text("Existing-file behavior", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExistingFilePolicy.entries.forEach { p ->
                    FilterChip(
                        selected = snap.existingPolicy == p,
                        onClick = { settings.appPrefs.setPolicy(p) },
                        label = { Text(p.name) }
                    )
                }
            }
            Row {
                Checkbox(snap.confirmBeforePush, { settings.appPrefs.setConfirm(it) })
                Text("Confirm before pushing", modifier = Modifier.padding(top = 12.dp))
            }
            Text("Upload retry count: ${snap.retryCount}")
            Slider(snap.retryCount.toFloat(), { settings.appPrefs.setRetry(it.toInt()) },
                valueRange = 0f..5f, steps = 4)
        }

        GlassCard(accent = accent) {
            SectionLabel("Appearance")
            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM", "DARK", "LIGHT").forEach { t ->
                    FilterChip(snap.themeMode == t, { onTheme(t) }, { Text(t) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Accent", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("CYAN", "VIOLET", "GREEN").forEach { a ->
                    FilterChip(a == accentName, { onAccent(a) }, { Text(a) })
                }
            }
        }

        GlassCard(accent = accent) {
            SectionLabel("Security")
            Text("Credentials are stored with Android Keystore-backed encryption (EncryptedSharedPreferences). The PAT is only sent to https://api.github.com and never logged.",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { settings.creds.clearTokenOnly() }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear PAT only")
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { scope.launch { settings.clearHistoryNow() } },
                modifier = Modifier.fillMaxWidth()) { Text("Clear local history") }
        }

        GlassCard(accent = accent) {
            SectionLabel("About")
            Text("GitPush 1.0.0", fontWeight = FontWeight.Bold)
            Text("GitHub REST API over HTTPS. No Termux, Git CLI, SSH, or backend required.",
                style = MaterialTheme.typography.bodySmall)
            Text("Your files are sent directly from your device to GitHub.",
                style = MaterialTheme.typography.bodySmall)
            Text("Privacy: no analytics. History stays on-device.",
                style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(70.dp))
    }

    if (showClearCreds) {
        AlertDialog(
            onDismissRequest = { showClearCreds = false },
            title = { Text("Clear credentials?") },
            text = { Text("This removes the saved token and repository configuration from this device.") },
            confirmButton = {
                TextButton(onClick = { settings.creds.clearCredentials(); showClearCreds = false }) {
                    Text("Clear")
                }
            },
            dismissButton = { TextButton(onClick = { showClearCreds = false }) { Text("Cancel") } }
        )
    }
}
