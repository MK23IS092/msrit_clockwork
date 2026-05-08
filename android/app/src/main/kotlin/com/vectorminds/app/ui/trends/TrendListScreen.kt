package com.vectorminds.app.ui.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vectorminds.app.ui.theme.*
import com.vectorminds.core.network.TrendItem

@Composable
fun TrendListScreen(
    onTrendClick: (String) -> Unit,
    viewModel: TrendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(16.dp)
    ) {
        Text(
            "Trend Leaderboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Emerging AI techniques ranked by hourly impact",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Source Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = true, onClick = {}, label = { Text("All Sources") })
            FilterChip(selected = false, onClick = {}, label = { Text("arXiv") })
            FilterChip(selected = false, onClick = {}, label = { Text("GitHub") })
            FilterChip(selected = false, onClick = { /* Patent implementation in Phase 2 */ }, label = { Text("Patents") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.searchTrends(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Semantic search (e.g. 'transformer novelty')") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.trends.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No trends yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Run an ingestion from the Dashboard to discover trends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(uiState.trends) { index, trend ->
                    TrendCard(
                        trend = trend,
                        rank = index + 1,
                        onClick = { onTrendClick(trend.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrendCard(
    trend: TrendItem,
    rank: Int,
    onClick: () -> Unit,
) {
    val scoreColor = when {
        trend.emergenceScore > 0.7 -> ScoreHigh
        trend.emergenceScore > 0.4 -> ScoreMedium
        else -> ScoreLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scoreColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    trend.techniqueName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    trend.description.take(100) + if (trend.description.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChipLabel("Score: ${"%.0f".format(trend.emergenceScore * 100)}%", scoreColor)
                    ChipLabel("ETA: ${trend.mainstreamEtaMonths}mo", CyanPrimary)
                    ChipLabel("${trend.paperCount} papers", PurpleSecondary)
                }
            }
        }
    }
}

@Composable
fun ChipLabel(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
