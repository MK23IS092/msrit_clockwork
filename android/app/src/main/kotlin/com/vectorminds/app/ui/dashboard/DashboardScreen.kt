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
import com.vectormind.app.ui.dashboard.components.*
import com.vectorminds.app.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTrends: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val galaxyPoints = remember { generateMockGalaxyPoints() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
    ) {
        TopAppBar(
            title = { Text("VectorMind", color = Color.White, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Situation Model (Phase 11 Integration)
            item {
                SituationModelPanel(
                    location = "Samsung R&D Lab",
                    focus = "Diffusion Transformers",
                    nextMeeting = "Dr. Arxiv: GNN Architectures @ 2:00 PM"
                )
            }

            // Proactive Author Alert
            item {
                AuthorAlertCard(
                    authorName = "Dr. Arxiv",
                    papersCount = 4,
                    onViewBrief = { /* TODO */ }
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

            // Research Galaxy 3.0
            item {
                Text(
                    "Research Galaxy", 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                    color = CyanPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ResearchGalaxy(galaxyPoints)
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
                    if (log.contains("brief") || log.contains("Author")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ReasoningPanel(
                            contextLabel = "Researcher Sync",
                            confidence = 0.96f,
                            reasoningPoints = listOf(
                                "Meeting 'GNN Discussion' starts in 15 mins",
                                "Attendee 'Dr. Arxiv' is a high-impact author",
                                "Semantic match with 4 newly ingested signals"
                            )
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Autonomous Ingestion Sync", fontWeight = FontWeight.Bold)
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
fun ActionLogItem(log: String) {
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
                    .background(if (log.contains("Success") || log.contains("complete")) SuccessGreen else WarningAmber)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(log, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}
