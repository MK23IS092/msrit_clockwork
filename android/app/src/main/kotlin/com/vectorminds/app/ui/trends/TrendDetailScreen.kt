package com.vectorminds.app.ui.trends

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.app.ui.components.VmButton
import com.vectorminds.app.ui.components.VmButtonStyle
import com.vectorminds.app.ui.components.VmCard
import com.vectorminds.app.ui.components.VmChip
import com.vectorminds.app.ui.components.VmChipStyle
import com.vectorminds.app.ui.components.VmMarkdown
import com.vectorminds.app.ui.components.VmScoreRing
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.core.network.TrendDetail
import com.vectorminds.core.network.VectorMindsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun TrendDetailScreen(
    trendId: String,
    onBack: () -> Unit,
    onShowBlueprints: () -> Unit = {},
    onShowPipelines: () -> Unit = {},
    viewModel: TrendDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(trendId) { viewModel.loadTrend(trendId) }
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isBriefLoading by viewModel.isBriefLoading.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val vm = Vm.colors

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TrendDetailEvent.BlueprintReady -> {
                    val res = snackbarHostState.showSnackbar(
                        message = "Blueprint generated for ${event.techniqueName}",
                        actionLabel = "View",
                        duration = SnackbarDuration.Short,
                    )
                    if (res == SnackbarResult.ActionPerformed) onShowBlueprints()
                }
                is TrendDetailEvent.PipelineReady -> {
                    val res = snackbarHostState.showSnackbar(
                        message = "ML pipeline generated · Colab ready",
                        actionLabel = "View",
                        duration = SnackbarDuration.Short,
                    )
                    if (res == SnackbarResult.ActionPerformed) onShowPipelines()
                }
                is TrendDetailEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = vm.background,
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(vm.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(vm.surfaceElevated)
                            .border(1.dp, vm.border, RoundedCornerShape(12.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = vm.textMuted, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "TREND DETAIL",
                        style = MaterialTheme.typography.labelSmall,
                        color = vm.brand,
                    )
                }
            }

            if (isLoading && detail == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = vm.brand) }
                }
            } else if (detail != null) {
                val d = detail!!

                item {
                    Text(
                        d.techniqueName,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = vm.text,
                    )
                }

                // Score panel — 4 rings + chip row, no overlap
                item { ScorePanel(d) }

                // Description
                if (d.description.isNotBlank()) {
                    item { SectionCard(title = "Description") { VmMarkdown(d.description) } }
                }

                // Technical brief — markdown rendered, with shimmer if pending
                item {
                    when {
                        !d.technicalBrief.isNullOrBlank() -> SectionCard(title = "Technical Brief") {
                            VmMarkdown(d.technicalBrief!!)
                        }
                        isBriefLoading -> VmCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = vm.brand,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Generating technical brief…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = vm.textMuted,
                                )
                            }
                        }
                    }
                }

                // Competitive landscape — FlowRow so chips wrap
                if (!d.competitiveLandscape.isNullOrEmpty()) {
                    item {
                        VmCard {
                            EyebrowLabel("Competes With")
                            Spacer(Modifier.height(10.dp))
                            ChipFlow(d.competitiveLandscape!!.take(8))
                        }
                    }
                }

                // Risk factors — bullets via markdown helper for consistency
                if (!d.riskFactors.isNullOrEmpty()) {
                    item {
                        VmCard {
                            EyebrowLabel("Risk Factors", color = vm.amber)
                            Spacer(Modifier.height(10.dp))
                            d.riskFactors!!.forEach { r ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        Modifier
                                            .padding(top = 8.dp)
                                            .size(4.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(vm.amber),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        r,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = vm.text,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // Related techniques
                if (!d.relatedTechniques.isNullOrEmpty()) {
                    item {
                        VmCard {
                            EyebrowLabel("Related Techniques")
                            Spacer(Modifier.height(10.dp))
                            ChipFlow(d.relatedTechniques!!.take(10))
                        }
                    }
                }

                // Actions
                item {
                    VmButton(
                        text = if (isGenerating) "Generating…" else "Generate Product Blueprint",
                        onClick = { viewModel.generateBlueprint(d.id, d.techniqueName) },
                        icon = Icons.Filled.AutoGraph,
                        loading = isGenerating,
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    VmButton(
                        text = "Launch ML Training Pipeline",
                        onClick = { viewModel.generatePipeline(d.techniqueName, d.description) },
                        style = VmButtonStyle.Secondary,
                        icon = Icons.Filled.Terminal,
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item { Spacer(Modifier.height(40.dp)) }
            } else {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Trend not found", color = vm.textMuted) }
                }
            }
        }
    }
}

// ─── Sub-components ──────────────────────────────────────────────────────────

@Composable
private fun ScorePanel(d: TrendDetail) {
    val vm = Vm.colors
    VmCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            VmScoreRing(progress = d.emergenceScore.toFloat(), label = "Emerge")
            VmScoreRing(progress = d.noveltyScore.toFloat(), label = "Novel", accent = vm.brand)
            VmScoreRing(progress = d.impactScore.toFloat(), label = "Impact", accent = vm.violet)
            VmScoreRing(progress = d.confidence.toFloat(), label = "Conf", accent = vm.emerald)
        }
        Spacer(Modifier.height(14.dp))
        ChipFlow(
            buildList {
                add("ETA ${d.mainstreamEtaMonths} mo")
                d.relatedTechniques?.size?.let { if (it > 0) add("$it related") }
                add("rank #${d.rank}")
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(items: List<String>) {
    if (items.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { VmChip(it, style = VmChipStyle.Outline) }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    VmCard {
        EyebrowLabel(title)
        Spacer(Modifier.height(10.dp))
        content()
    }
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

// ─── ViewModel & events ──────────────────────────────────────────────────────

sealed interface TrendDetailEvent {
    data class BlueprintReady(val techniqueName: String) : TrendDetailEvent
    data class PipelineReady(val techniqueName: String) : TrendDetailEvent
    data class Error(val message: String) : TrendDetailEvent
}

@HiltViewModel
class TrendDetailViewModel @Inject constructor(
    private val api: VectorMindsApi,
) : ViewModel() {

    private val _detail = MutableStateFlow<TrendDetail?>(null)
    val detail: StateFlow<TrendDetail?> = _detail

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isBriefLoading = MutableStateFlow(false)
    val isBriefLoading: StateFlow<Boolean> = _isBriefLoading

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _events = MutableSharedFlow<TrendDetailEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<TrendDetailEvent> = _events.asSharedFlow()

    fun loadTrend(trendId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _isBriefLoading.value = false
            _detail.value = null
            try {
                _detail.value = api.getTrendDetail(trendId, includeBrief = false)
            } catch (e: Exception) {
                _events.tryEmit(TrendDetailEvent.Error("Couldn't load trend: ${e.message ?: "network error"}"))
            }
            _isLoading.value = false

            launch {
                _isBriefLoading.value = true
                try {
                    val withBrief = api.getTrendDetail(trendId, includeBrief = true)
                    _detail.value = withBrief
                } catch (_: Exception) {
                    // Keep the fast payload; brief is optional.
                } finally {
                    _isBriefLoading.value = false
                }
            }
        }
    }

    fun generateBlueprint(trendId: String, techniqueName: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                api.generateBlueprint(com.vectorminds.core.network.BlueprintRequest(trendId = trendId))
                _events.tryEmit(TrendDetailEvent.BlueprintReady(techniqueName))
            } catch (e: Exception) {
                _events.tryEmit(TrendDetailEvent.Error("Blueprint failed: ${e.message ?: "unknown error"}"))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generatePipeline(name: String, desc: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                api.generatePipeline(com.vectorminds.core.network.PipelineRequest(techniqueName = name, description = desc))
                _events.tryEmit(TrendDetailEvent.PipelineReady(name))
            } catch (e: Exception) {
                _events.tryEmit(TrendDetailEvent.Error("Pipeline failed: ${e.message ?: "unknown error"}"))
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
