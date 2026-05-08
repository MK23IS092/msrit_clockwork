package com.vectorminds.app.ui.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vectorminds.app.ui.common.OnResumeEffect
import com.vectorminds.app.ui.components.VmButton
import com.vectorminds.app.ui.components.VmButtonSize
import com.vectorminds.app.ui.components.VmButtonStyle
import com.vectorminds.app.ui.components.VmCard
import com.vectorminds.app.ui.components.VmChip
import com.vectorminds.app.ui.components.VmChipStyle
import com.vectorminds.app.ui.components.VmEmptyState
import com.vectorminds.app.ui.components.VmFilterChip
import com.vectorminds.app.ui.components.VmScreenHeader
import com.vectorminds.app.ui.components.VmScreenSurface
import com.vectorminds.app.ui.components.VmShimmerBox
import com.vectorminds.app.ui.components.VmSparkline
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.core.network.TrendItem

@Composable
fun TrendListScreen(
    onTrendClick: (String) -> Unit,
    viewModel: TrendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val vm = Vm.colors

    OnResumeEffect { viewModel.refreshIfIdle() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("All") }

    VmScreenSurface(bottomPadding = 96.dp) {
        Column(Modifier.fillMaxSize()) {
            VmScreenHeader(
                eyebrow = "Intelligence",
                title = "Trend Leaderboard",
                subtitle = "Emerging AI techniques ranked by emergence score",
                trailing = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(vm.surfaceElevated)
                            .border(1.dp, vm.border, RoundedCornerShape(12.dp))
                            .clickable { viewModel.loadTrends() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = vm.textMuted, modifier = Modifier.size(16.dp))
                    }
                },
            )

            Spacer(Modifier.height(16.dp))

            // Search bar
            VmSearchBar(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchTrends(it)
                },
                placeholder = "Semantic search · transformer novelty…",
            )

            Spacer(Modifier.height(12.dp))

            // Source filters
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "arXiv", "GitHub").forEach { src ->
                    VmFilterChip(
                        text = src,
                        selected = selectedSource == src,
                        onClick = { selectedSource = src },
                    )
                }
                VmFilterChip(text = "Patents", selected = false, onClick = {}, enabled = false)
            }

            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading && uiState.trends.isEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) { TrendCardSkeleton() }
                    }
                }
                uiState.trends.isEmpty() -> {
                    VmEmptyState(
                        icon = Icons.Filled.TrendingUp,
                        title = if (uiState.error != null) "Couldn't load trends" else "No trends yet",
                        subtitle = uiState.error
                            ?: "Run an ingestion from the Command Center to populate the leaderboard.",
                        primaryAction = {
                            VmButton(
                                text = "Retry",
                                onClick = { viewModel.loadTrends() },
                                style = VmButtonStyle.Secondary,
                                size = VmButtonSize.Sm,
                                icon = Icons.Filled.Refresh,
                            )
                        },
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(uiState.trends) { i, trend ->
                            TrendCard(
                                trend = trend,
                                rank = i + 1,
                                onClick = { onTrendClick(trend.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Trend card ───────────────────────────────────────────────────────────────

@Composable
private fun TrendCard(trend: TrendItem, rank: Int, onClick: () -> Unit) {
    val vm = Vm.colors
    val band = when {
        trend.emergenceScore > 0.7 -> vm.scoreHigh
        trend.emergenceScore > 0.4 -> vm.scoreMid
        else -> vm.scoreLow
    }

    VmCard(onClick = onClick, accent = band, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rank
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(band.copy(alpha = 0.14f))
                    .border(1.dp, band.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = band,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    trend.techniqueName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = vm.text,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (trend.description.length > 90)
                        trend.description.take(90).trimEnd() + "…"
                    else trend.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = vm.textMuted,
                    maxLines = 2,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VmChip(
                        text = "S ${(trend.emergenceScore * 100).toInt()}",
                        tint = band,
                        style = VmChipStyle.Soft,
                    )
                    VmChip(
                        text = "ETA ${trend.mainstreamEtaMonths}mo",
                        tint = vm.brand,
                        style = VmChipStyle.Outline,
                    )
                    VmChip(
                        text = "${trend.paperCount}p",
                        tint = vm.violet,
                        style = VmChipStyle.Outline,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(trend.emergenceScore * 100).toInt()}",
                    fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = band,
                )
                Spacer(Modifier.height(2.dp))
                VmSparkline(
                    color = band,
                    seed = rank,
                    modifier = Modifier
                        .width(72.dp)
                        .height(28.dp),
                )
            }
        }
    }
}

@Composable
private fun TrendCardSkeleton() {
    val vm = Vm.colors
    VmCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(vm.surfaceElevated),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                Spacer(Modifier.height(8.dp))
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(10.dp))
                Spacer(Modifier.height(4.dp))
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(10.dp))
            }
        }
    }
}

@Composable
private fun VmSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val vm = Vm.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(vm.surfaceElevated)
            .border(1.dp, vm.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = vm.textFaint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = vm.textFaint,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(vm.brand),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = vm.text),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
