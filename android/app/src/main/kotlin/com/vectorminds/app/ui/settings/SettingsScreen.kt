package com.vectorminds.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.*

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // Backend Config
        SettingsSection("Backend Connection") {
            SettingsRow("Server URL", "http://10.0.2.2:8000")
            SettingsRow("LLM Provider", "Groq (Free Tier)")
            SettingsRow("LLM Model", "llama-3.1-70b-versatile")
        }

        Spacer(Modifier.height(16.dp))

        // Agent Skills
        SettingsSection("Agent Skills") {
            SettingsToggle("Trend Monitor", "Auto-ingest research papers", true)
            SettingsToggle("Blueprint Generator", "Auto-generate product blueprints", true)
            SettingsToggle("Pipeline Launcher", "Auto-generate ML training pipelines", false)
            SettingsToggle("Telegram Alerts", "Push notifications for high-impact trends", false)
        }

        Spacer(Modifier.height(16.dp))

        // Data Sources
        SettingsSection("Data Sources") {
            SettingsRow("arXiv Categories", "cs.LG, cs.AI, cs.CL, cs.CV")
            SettingsRow("GitHub Topics", "machine-learning, deep-learning")
            SettingsRow("Embedding Model", "BGE-Small-EN-v1.5")
        }

        Spacer(Modifier.height(16.dp))

        // About
        SettingsSection("About") {
            SettingsRow("Version", "1.0.0-hackathon")
            SettingsRow("Platform", "OpenClaw Multi-Agent Orchestration")
            SettingsRow("License", "MIT")
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String, default: Boolean) {
    var checked by remember { mutableStateOf(default) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}
