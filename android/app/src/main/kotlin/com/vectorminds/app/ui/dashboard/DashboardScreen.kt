package com.vectorminds.app.ui.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.vectorminds.app.ui.dashboard.components.GalaxyPoint
import com.vectorminds.app.ui.dashboard.components.ResearchGalaxy
import com.vectorminds.app.ui.dashboard.components.generateMockGalaxyPoints
import com.vectorminds.app.ui.components.VmButton
import com.vectorminds.app.ui.components.VmButtonSize
import com.vectorminds.app.ui.components.VmButtonStyle
import com.vectorminds.app.ui.components.VmCard
import com.vectorminds.app.ui.components.VmChip
import com.vectorminds.app.ui.components.VmChipStyle
import com.vectorminds.app.ui.components.VmProgressBar
import com.vectorminds.app.ui.components.VmScreenHeader
import com.vectorminds.app.ui.components.VmScreenSurface
import com.vectorminds.app.ui.components.VmSparkline
import com.vectorminds.app.ui.components.VmStat
import com.vectorminds.app.ui.components.VmStatusPill
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.core.data.db.entity.ActionLogEntity

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTrends: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsState()
    val vm = Vm.colors

    val galaxyPoints = remember(ui.galaxyPoints) {
        if (ui.galaxyPoints.isNotEmpty()) {
            ui.galaxyPoints.take(30).map { p ->
                GalaxyPoint(
                    x = p.x.toFloat(),
                    y = p.y.toFloat(),
                    label = p.title,
                    category = p.source,
                    score = p.noveltyScore.toFloat(),
                )
            }
        } else generateMockGalaxyPoints()
    }

    var galaxyReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        galaxyReady = true
    }

    VmScreenSurface {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                VmScreenHeader(
                    eyebrow = "Command Center",
                    title = "VectorMind",
                    subtitle = "Autonomous research intelligence",
                    trailing = {
                        VmStatusPill(
                            label = if (ui.isBackendOnline) "Live" else "Offline",
                            healthy = ui.isBackendOnline,
                        )
                    },
                )
            }

            // KPI grid: 4 stats in 2 rows of 2
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VmStat(
                        label = "Papers",
                        value = ui.totalPapers.toString(),
                        icon = Icons.Filled.Article,
                        accent = vm.brand,
                        modifier = Modifier.weight(1f),
                    )
                    VmStat(
                        label = "Repos",
                        value = ui.totalRepos.toString(),
                        icon = Icons.Filled.Code,
                        accent = vm.violet,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VmStat(
                        label = "Active Trends",
                        value = ui.activeTrends.toString(),
                        icon = Icons.Filled.TrendingUp,
                        accent = vm.emerald,
                        modifier = Modifier.weight(1f),
                    )
                    VmStat(
                        label = "Avg Novelty",
                        value = "${(ui.avgNovelty * 100).toInt()}%",
                        icon = Icons.Filled.Bolt,
                        accent = vm.amber,
                        sublabel = "rolling 24h",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Situation Model
            item {
                SituationCard(
                    location = ui.situationLocation,
                    focus = ui.situationFocus,
                    nextMeeting = ui.situationMeeting,
                )
            }

            // Reasoning summary
            if (ui.reasoningPoints.isNotEmpty()) {
                item {
                    ReasoningCard(
                        confidence = ui.reasoningConfidence,
                        points = ui.reasoningPoints,
                        focus = ui.situationFocus,
                    )
                }
            }

            // Author / signal alert
            if (ui.authorName.isNotBlank() && ui.authorName != "Loading...") {
                item {
                    AuthorAlertCard(
                        author = ui.authorName,
                        papersCount = ui.authorPapersCount,
                        onView = onNavigateToTrends,
                    )
                }
            }

            // Research Galaxy
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(vm.brandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.Polyline, null,
                                tint = vm.brand,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Research Galaxy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = vm.text,
                        )
                        Spacer(Modifier.weight(1f))
                        VmChip(
                            text = "${galaxyPoints.size} clusters",
                            style = VmChipStyle.Outline,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    VmCard(contentPadding = 0.dp) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF050608)),
                        ) {
                            if (galaxyReady) ResearchGalaxy(galaxyPoints)
                        }
                    }
                }
            }

            // Ingestion control
            item {
                IngestionCard(
                    isIngesting = ui.isIngesting,
                    statusText = ui.lastIngestionResult,
                    onTrigger = { viewModel.triggerIngestion() },
                    onExploreTrends = onNavigateToTrends,
                )
            }

            // Agent activity feed
            if (ui.agentLogs.isNotEmpty()) {
                item {
                    Text(
                        "Agent Intelligence Feed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = vm.text,
                    )
                }
                items(ui.agentLogs.take(6)) { log ->
                    AgentLogRow(log)
                }
            }
        }
    }
}

// ─── Sub-cards ───────────────────────────────────────────────────────────────

@Composable
private fun SituationCard(location: String, focus: String, nextMeeting: String) {
    val vm = Vm.colors
    VmCard(accent = vm.brand) {
        Column {
            Text(
                "SITUATION MODEL",
                style = MaterialTheme.typography.labelSmall,
                color = vm.brand,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                focus.ifBlank { "—" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = vm.text,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (location.isNotBlank()) VmChip(location, style = VmChipStyle.Outline)
                if (nextMeeting.isNotBlank()) VmChip(
                    nextMeeting,
                    style = VmChipStyle.Soft,
                    tint = vm.violet,
                )
            }
        }
    }
}

@Composable
private fun ReasoningCard(confidence: Float, points: List<String>, focus: String) {
    val vm = Vm.colors
    VmCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Filled.Memory, null,
                    tint = vm.violet,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "REASONING",
                    style = MaterialTheme.typography.labelSmall,
                    color = vm.violet,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${(confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = vm.textMuted,
                )
            }
            Spacer(Modifier.height(8.dp))
            VmProgressBar(progress = confidence, color = vm.violet)
            Spacer(Modifier.height(12.dp))
            points.take(3).forEach { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 7.dp)
                            .size(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(vm.violet),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        p,
                        style = MaterialTheme.typography.bodyMedium,
                        color = vm.textMuted,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AuthorAlertCard(author: String, papersCount: Int, onView: () -> Unit) {
    val vm = Vm.colors
    VmCard(accent = vm.amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(vm.amber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.NotificationsActive, null,
                    tint = vm.amber,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Hot signal",
                    style = MaterialTheme.typography.labelSmall,
                    color = vm.amber,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    author,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = vm.text,
                )
                Text(
                    "$papersCount papers in window",
                    style = MaterialTheme.typography.bodySmall,
                    color = vm.textMuted,
                )
            }
            VmButton(
                text = "View",
                onClick = onView,
                style = VmButtonStyle.Ghost,
                size = VmButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun IngestionCard(
    isIngesting: Boolean,
    statusText: String,
    onTrigger: () -> Unit,
    onExploreTrends: () -> Unit,
) {
    val vm = Vm.colors
    VmCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "INGESTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = vm.textFaint,
                )
                Spacer(Modifier.weight(1f))
                VmSparkline(
                    color = vm.brand,
                    modifier = Modifier
                        .width(96.dp)
                        .height(24.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Sync research signals",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = vm.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pull arXiv, GitHub, patents, and startup signals; re-rank trends.",
                style = MaterialTheme.typography.bodySmall,
                color = vm.textMuted,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VmButton(
                    text = if (isIngesting) "Ingesting…" else "Run Ingestion",
                    onClick = onTrigger,
                    icon = Icons.Filled.AutoMode,
                    loading = isIngesting,
                    enabled = !isIngesting,
                    modifier = Modifier.weight(1f),
                )
                VmButton(
                    text = "Trends",
                    icon = Icons.Filled.Insights,
                    style = VmButtonStyle.Secondary,
                    onClick = onExploreTrends,
                    modifier = Modifier.weight(1f),
                )
            }
            if (statusText.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = vm.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AgentLogRow(log: ActionLogEntity) {
    val vm = Vm.colors
    val statusColor = when (log.status) {
        "success" -> vm.success
        "failed", "error" -> vm.danger
        else -> vm.amber
    }
    VmCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                log.description,
                style = MaterialTheme.typography.bodyMedium,
                color = vm.text,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                log.skillId,
                style = MaterialTheme.typography.labelSmall,
                color = vm.textFaint,
            )
        }
    }
}
