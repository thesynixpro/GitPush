package com.aprax.gitpush.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aprax.gitpush.model.ExistingFilePolicy
import com.aprax.gitpush.ui.components.*
import com.aprax.gitpush.ui.theme.accentFor
import com.aprax.gitpush.viewmodel.PushSessionViewModel

@Composable
fun RepoConfigScreen(
    vm: PushSessionViewModel,
    accentName: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val accent = accentFor(accentName)
    var owner by remember(vm.owner) { mutableStateOf(vm.owner) }
    var repo by remember(vm.repo) { mutableStateOf(vm.repo) }
    var branch by remember(vm.branch) { mutableStateOf(vm.branch) }
    var dest by remember(vm.destPath) { mutableStateOf(vm.destPath) }
    var user by remember(vm.username) { mutableStateOf(vm.username) }
    var token by remember { mutableStateOf("") }
    var commit by remember(vm.commitMessage) { mutableStateOf(vm.commitMessage) }
    var showToken by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }

    fun sync() = vm.setField(owner, repo, branch, dest, user, token, commit)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("GitHub repository", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Fine-grained PATs need repository access with permission to read metadata and write contents. Classic tokens need the repo scope. Only the minimum access is used.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        GlassCard(accent = accent) {
            SectionLabel("GitHub account (PAT only)")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(user, { user = it; sync() }, Modifier.fillMaxWidth(), label = { Text("GitHub username") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                token, { token = it; sync() }, Modifier.fillMaxWidth(),
                label = { Text(if (vm.hasToken) "New token (leave blank to keep saved)" else "Personal Access Token") },
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showToken) "Hide token" else "Show token")
                    }
                },
                singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your Personal Access Token is sensitive. Never share it. It is stored encrypted on this device and only sent to api.github.com over HTTPS.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
            )
        }

        GlassCard(accent = accent) {
            SectionLabel("Repository")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(owner, { owner = it; sync() }, Modifier.fillMaxWidth(), label = { Text("Repository owner") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(repo, { repo = it; sync() }, Modifier.fillMaxWidth(), label = { Text("Repository name") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(branch, { branch = it; sync() }, Modifier.fillMaxWidth(), label = { Text("Branch") }, placeholder = { Text("main") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(dest, { dest = it; sync() }, Modifier.fillMaxWidth(), label = { Text("Destination path (optional)") }, placeholder = { Text("optional/path — blank = root") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(commit, { commit = it; sync() }, Modifier.fillMaxWidth(), label = { Text("Commit message") }, singleLine = false, minLines = 1)
            Spacer(Modifier.height(8.dp))
            SectionLabel("If a file already exists")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExistingFilePolicy.entries.forEach { p ->
                    FilterChip(
                        selected = vm.policy == p,
                        onClick = { vm.updatePolicy(p) },
                        label = { Text(p.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                    )
                }
            }
        }

        msg?.let { Text(it, color = if (msgOk == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    sync()
                    vm.testConnection { r -> msg = r.message; msgOk = r.ok }
                },
                modifier = Modifier.weight(1f),
                enabled = !vm.testing
            ) { Text(if (vm.testing) "Testing…" else "Test Connection") }
            OutlinedButton(
                onClick = {
                    sync()
                    vm.verifyRepo { r ->
                        msg = if (r.ok) "Connected successfully — ${r.repoFullName}"
                        else r.message
                        msgOk = r.ok
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !vm.testing
            ) { Text("Test Repository") }
        }

        GradientButton(
            "Save & Continue",
            onClick = {
                sync()
                if (!vm.storageAvailable) {
                    msg = "Secure storage is unavailable on this device, so the token cannot be saved safely."
                    msgOk = false
                } else if (vm.saveConfiguration()) onNext()
                else { msg = "Fill owner, repo and token first."; msgOk = false }
            },
            accent = accent
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        Spacer(Modifier.height(70.dp))
    }
}
