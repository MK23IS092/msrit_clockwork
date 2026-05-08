package com.vectorminds.app.ui.blueprints

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.app.ui.common.OnResumeEffect
import com.vectorminds.app.ui.components.VmButton
import com.vectorminds.app.ui.components.VmButtonSize
import com.vectorminds.app.ui.components.VmButtonStyle
import com.vectorminds.app.ui.components.VmCard
import com.vectorminds.app.ui.components.VmChip
import com.vectorminds.app.ui.components.VmChipStyle
import com.vectorminds.app.ui.components.VmEmptyState
import com.vectorminds.app.ui.components.VmMarkdown
import com.vectorminds.app.ui.components.VmScreenHeader
import com.vectorminds.app.ui.components.VmScreenSurface
import com.vectorminds.app.ui.components.VmShimmerBox
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.core.network.BlueprintResponse
import com.vectorminds.core.network.VectorMindsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun BlueprintScreen(viewModel: BlueprintViewModel = hiltViewModel()) {
    val blueprints by viewModel.blueprints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val vm = Vm.colors

    OnResumeEffect { viewModel.refresh() }

    VmScreenSurface {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                VmScreenHeader(
                    eyebrow = "Strategy",
                    title = "Product Blueprints",
                    subtitle = "Investor-grade dossiers from emerging techniques",
                    trailing = {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(vm.surfaceElevated)
                                .border(1.dp, vm.border, RoundedCornerShape(12.dp))
                                .clickable { viewModel.refresh() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Refresh, "Refresh",
                                tint = vm.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }

            when {
                isLoading && blueprints.isEmpty() -> items(3) { BlueprintSkeleton() }
                blueprints.isEmpty() -> item {
                    VmEmptyState(
                        icon = Icons.Filled.AutoGraph,
                        title = if (error != null) "Couldn't load blueprints" else "No blueprints yet",
                        subtitle = error
                            ?: "Open a trend and tap “Generate Product Blueprint” to mint one.",
                        primaryAction = {
                            VmButton(
                                text = "Retry",
                                onClick = { viewModel.refresh() },
                                style = VmButtonStyle.Secondary,
                                size = VmButtonSize.Sm,
                                icon = Icons.Filled.Refresh,
                            )
                        },
                    )
                }
                else -> items(blueprints, key = { it.id }) { bp -> BlueprintCard(bp) }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun BlueprintCard(bp: BlueprintResponse) {
    val vm = Vm.colors
    var expanded by remember(bp.id) { mutableStateOf(false) }
    val rot by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "bpExpand",
    )

    VmCard(onClick = { expanded = !expanded }) {
        Column(Modifier.animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(vm.brandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoGraph, null, tint = vm.brand, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        bp.techniqueName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = vm.text,
                        maxLines = 2,
                    )
                    if (bp.marketSize.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Market · ${bp.marketSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = vm.emerald,
                            maxLines = 1,
                        )
                    }
                }
                Icon(
                    Icons.Filled.ExpandMore, null,
                    tint = vm.textMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rot),
                )
            }

            Spacer(Modifier.height(10.dp))
            // Problem statement preview / full
            if (bp.problemStatement.isNotBlank()) {
                if (expanded) {
                    EyebrowLabel("Problem")
                    Spacer(Modifier.height(6.dp))
                    VmMarkdown(bp.problemStatement)
                } else {
                    Text(
                        if (bp.problemStatement.length > 180)
                            bp.problemStatement.take(180).trimEnd() + "…"
                        else bp.problemStatement,
                        style = MaterialTheme.typography.bodySmall,
                        color = vm.textMuted,
                    )
                }
            }

            if (expanded) {
                MarkdownSection("Technical Implementation", bp.technicalImplementation)
                MarkdownSection("Differentiation", bp.differentiationStrategy)
                MarkdownSection("Dataset Strategy", bp.datasetRequirements)
                MarkdownSection("Go-to-Market", bp.goToMarket)
                MarkdownSection("Risk Assessment", bp.riskAssessment, accent = vm.amber)

                if (bp.architectureDecisions.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    EyebrowLabel("Architecture Decisions")
                    Spacer(Modifier.height(8.dp))
                    bp.architectureDecisions.forEach { d ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .padding(top = 8.dp)
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(vm.brand),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                d,
                                style = MaterialTheme.typography.bodyMedium,
                                color = vm.text,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (bp.first90DayMilestones.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    EyebrowLabel("First 90 Days")
                    Spacer(Modifier.height(8.dp))
                    bp.first90DayMilestones.forEachIndexed { i, m ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "${i + 1}.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = vm.brand,
                                modifier = Modifier.width(22.dp),
                            )
                            Text(
                                m,
                                style = MaterialTheme.typography.bodyMedium,
                                color = vm.text,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (bp.suggestedStack.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    EyebrowLabel("Suggested Stack")
                    Spacer(Modifier.height(8.dp))
                    StackChips(bp.suggestedStack)
                }
            }
        }
    }
}

@Composable
private fun MarkdownSection(
    title: String,
    content: String,
    accent: androidx.compose.ui.graphics.Color? = null,
) {
    if (content.isBlank()) return
    Spacer(Modifier.height(14.dp))
    EyebrowLabel(title, accent)
    Spacer(Modifier.height(8.dp))
    VmMarkdown(content)
}

@Composable
private fun EyebrowLabel(text: String, color: androidx.compose.ui.graphics.Color? = null) {
    val vm = Vm.colors
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: vm.textFaint,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StackChips(items: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { VmChip(it, style = VmChipStyle.Outline) }
    }
}

@Composable
private fun BlueprintSkeleton() {
    val vm = Vm.colors
    VmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VmShimmerBox(modifier = Modifier.size(40.dp), cornerRadius = 12.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
                Spacer(Modifier.height(6.dp))
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        VmShimmerBox(modifier = Modifier.fillMaxWidth(0.95f).height(10.dp))
        Spacer(Modifier.height(4.dp))
        VmShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(10.dp))
    }
}

@HiltViewModel
class BlueprintViewModel @Inject constructor(
    private val api: VectorMindsApi,
) : ViewModel() {

    private val _blueprints = MutableStateFlow<List<BlueprintResponse>>(emptyList())
    val blueprints: StateFlow<List<BlueprintResponse>> = _blueprints

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _blueprints.value = api.listBlueprints().blueprints
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load blueprints"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
