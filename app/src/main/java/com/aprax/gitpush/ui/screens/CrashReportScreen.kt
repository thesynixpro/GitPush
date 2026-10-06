package com.aprax.gitpush.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aprax.gitpush.ui.components.GlassCard
import com.aprax.gitpush.ui.theme.accentFor

/** Shown on launch when a previous run crashed. Lets the user copy the trace. */
@Composable
fun CrashReportScreen(
    reportText: String,
    accentName: String,
    onCopy: () -> Unit,
    onContinue: () -> Unit
) {
    val accent = accentFor(accentName)
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "The app crashed last time",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Copy the log below and share it so the crash can be fixed. " +
                "It contains only device info and the error — no tokens or files.",
            style = MaterialTheme.typography.bodySmall
        )
        GlassCard(accent = accent, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text(
                reportText.take(12000),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
        Button(
            onClick = {
                runCatching { clipboard.setText(AnnotatedString(reportText.take(120000))) }
                onCopy()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Copy crash log") }
        OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Clear & continue to app")
        }
    }
}
