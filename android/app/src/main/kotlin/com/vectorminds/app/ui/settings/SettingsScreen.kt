package com.vectorminds.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.components.VmCard
import com.vectorminds.app.ui.components.VmChip
import com.vectorminds.app.ui.components.VmChipStyle
import com.vectorminds.app.ui.components.VmScreenHeader
import com.vectorminds.app.ui.components.VmScreenSurface
import com.vectorminds.app.ui.theme.Vm

@Composable
fun SettingsScreen() {
    val vm = Vm.colors

    VmScreenSurface {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                VmScreenHeader(
                    eyebrow = "Operator",
                    title = "Control Center",
                    subtitle = "Tune your autonomous research stack",
                )
            }

            // Profile / health
            item {
                VmCard(accent = vm.brand) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(vm.brandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Hub, null, tint = vm.brand, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "VectorMind",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = vm.text,
                            )
                            Text(
                                "Multi-agent · Edge-cloud hybrid",
                                style = MaterialTheme.typography.bodySmall,
                                color = vm.textMuted,
                            )
                        }
                        VmChip(text = "Healthy", tint = vm.success, style = VmChipStyle.Soft)
                    }
                }
            }

            // Backend connection
            item {
                Section("Backend Connection", Icons.Filled.Dns, vm.brand) {
                    SettingRow("Server URL", "configurable in local.properties")
                    SettingRow("LLM Provider", "Gemini (multi-model)")
                    SettingRow("Embedding Model", "BGE-Small-EN-v1.5")
                    SettingRow("Vector Store", "Qdrant")
                }
            }

            // Agent Skills
            item {
                Section("Agent Skills", Icons.Filled.Memory, vm.violet) {
                    var s1 by remember { mutableStateOf(true) }
                    var s2 by remember { mutableStateOf(true) }
                    var s3 by remember { mutableStateOf(false) }
                    var s4 by remember { mutableStateOf(true) }
                    SettingToggle("Trend Monitor", "Auto-ingest research papers", s1) { s1 = it }
                    SettingToggle("Blueprint Generator", "Auto-generate product blueprints", s2) { s2 = it }
                    SettingToggle("Pipeline Launcher", "Auto-generate ML training pipelines", s3) { s3 = it }
                    SettingToggle("Telegram Alerts", "Push notifications for high-impact trends", s4) { s4 = it }
                }
            }

            // Data sources
            item {
                Section("Data Sources", Icons.Filled.Bolt, vm.emerald) {
                    SettingRow("arXiv Categories", "cs.LG, cs.AI, cs.CL, cs.CV")
                    SettingRow("GitHub Topics", "machine-learning, deep-learning")
                    SettingRow("Patents", "USPTO weekly")
                    SettingRow("Startup Signals", "Crunchbase, ProductHunt")
                }
            }

            // Performance
            item {
                Section("Performance", Icons.Filled.Tune, vm.amber) {
                    SettingRow("Galaxy points", "30 (responsive)")
                    SettingRow("Trend cache TTL", "10 min")
                    SettingRow("Network timeout", "20s")
                }
            }

            // Privacy
            item {
                Section("Privacy & Security", Icons.Filled.PrivacyTip, vm.brand) {
                    SettingRow("Telemetry", "Off")
                    SettingRow("On-device cache", "Encrypted (AES-256)")
                    SettingRow("API keys", "Backend-managed")
                }
            }

            // About
            item {
                Section("About", Icons.Filled.Hub, vm.textMuted) {
                    SettingRow("Version", "1.0.0")
                    SettingRow("Platform", "OpenClaw Multi-Agent Orchestration")
                    SettingRow("License", "MIT")
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val vm = Vm.colors
    VmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = vm.textFaint,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    val vm = Vm.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = vm.text)
        Text(value, style = MaterialTheme.typography.bodySmall, color = vm.textMuted)
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val vm = Vm.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = vm.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = vm.textMuted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = vm.brand,
                checkedTrackColor = vm.brandSoft,
                checkedBorderColor = vm.brand,
                uncheckedThumbColor = vm.textFaint,
                uncheckedTrackColor = vm.surfaceElevated,
                uncheckedBorderColor = vm.border,
            ),
        )
    }
}
