package com.vectorminds.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vectorminds.app.ui.dashboard.components.*
import com.vectorminds.app.ui.theme.*
import com.vectorminds.core.data.db.entity.ActionLogEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTrends: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val galaxyPoints = remember(uiState.galaxyPoints) {
        if (uiState.galaxyPoints.isNotEmpty()) {
            // Cap to 30 points — the Galaxy renders an outer glow + core
            // circle per point each frame, plus relationship lines. Keeping
            // it under 30 keeps the first frame well under 16ms even on a
            // software-renderer emulator.
            uiState.galaxyPoints.take(30).map { point ->
                GalaxyPoint(
                    x = point.x.toFloat(),
                    y = point.y.toFloat(),
                    label = point.title,
                    category = point.source,
                    score = point.noveltyScore.toFloat(),
                )
            }
        } else {
            generateMockGalaxyPoints()
        }
    }

    // Defer the heavy ResearchGalaxy Canvas (two infinite transitions +
    // up to 30 points + 50 starfield stars) until AFTER the first frame
    // is on screen, so the splash dismisses immediately.
    var galaxyReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        galaxyReady = true
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A)),
        containerColor = Color(0xFF0F0F1A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "VectorMind",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F1A),
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Situation Model (Phase 11 Integration)
            item {
                SituationModelPanel(
                    location = uiState.situationLocation,
                    focus = uiState.situationFocus,
                    nextMeeting = uiState.situationMeeting
                )
            }

            // Proactive Author Alert
            item {
                AuthorAlertCard(
                    authorName = uiState.authorName,
                    papersCount = uiState.authorPapersCount,
                    onViewBrief = onNavigateToTrends
                )
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(Modifier.weight(1f), Icons.Default.Article, "Papers", "${uiState.totalPapers}", CyanPrimary)
                    StatCard(Modifier.weight(1f), Icons.Default.Code, "Repos", "${uiState.totalRepos}", PurpleSecondary)
                }
            }

            // Research Galaxy 3.0 — heavy Canvas, gated behind a 150ms
            // delay so the first frame can paint before the infinite
            // transitions kick in.
            item {
                Text(
                    "Research Galaxy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (galaxyReady) {
                    ResearchGalaxy(galaxyPoints)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF050510))
                    )
                }
            }

            // Action Log Header
            item {
                Text(
                    "Agent Intelligence Feed", 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                    color = PurpleSecondary
                )
            }

            // Action Log Items with Reasoning Panels
            items(uiState.agentLogs.take(5)) { log ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    ActionLogItem(log)
                    if (log.description.contains("brief", ignoreCase = true) ||
                        log.description.contains("author", ignoreCase = true)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ReasoningPanel(
                            contextLabel = uiState.situationFocus,
                            confidence = uiState.reasoningConfidence.coerceIn(0f, 1f),
                            reasoningPoints = if (uiState.reasoningPoints.isNotEmpty()) {
                                uiState.reasoningPoints
                            } else {
                                listOf("Awaiting backend intelligence context...")
                            }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.triggerIngestion() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    enabled = !uiState.isIngesting
                ) {
                    if (uiState.isIngesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Ingesting…", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Autonomous Ingestion Sync", fontWeight = FontWeight.Bold)
                    }
                }
                if (uiState.lastIngestionResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E2E).copy(alpha = 0.6f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            uiState.lastIngestionResult,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                OutlinedButton(
                    onClick = onNavigateToTrends,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Explore Hourly Trend Leaderboard", color = Color.LightGray)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ActionLogItem(log: ActionLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (log.status == "success") SuccessGreen else WarningAmber
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(log.description, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}
