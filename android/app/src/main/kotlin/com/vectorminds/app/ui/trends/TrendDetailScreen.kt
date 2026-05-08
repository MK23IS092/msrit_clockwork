package com.vectorminds.app.ui.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.*
import com.vectorminds.core.network.TrendDetail
import com.vectorminds.core.network.VectorMindsApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    viewModel: TrendDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(trendId) { viewModel.loadTrend(trendId) }
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isBriefLoading by viewModel.isBriefLoading.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

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
                        message = "ML pipeline generated • Colab ready",
                        actionLabel = "View",
                        duration = SnackbarDuration.Short,
                    )
                    if (res == SnackbarResult.ActionPerformed) onShowPipelines()
                }
                is TrendDetailEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }

            if (isLoading && detail == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else if (detail != null) {
                val d = detail!!

                Text(d.techniqueName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreChip(Modifier.weight(1f), "Emergence", d.emergenceScore, ScoreHigh)
                    ScoreChip(Modifier.weight(1f), "Novelty", d.noveltyScore, CyanPrimary)
                    ScoreChip(Modifier.weight(1f), "Impact", d.impactScore, PurpleSecondary)
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreChip(Modifier.weight(1f), "Confidence", d.confidence, EmeraldTertiary)
                    ScoreChip(Modifier.weight(1f), "ETA", d.mainstreamEtaMonths.toDouble() / 24, WarningAmber, "${d.mainstreamEtaMonths}mo")
                }

                Spacer(Modifier.height(16.dp))

                SectionCard("Description", d.description)

                if (isBriefLoading && d.technicalBrief.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyanPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Generating technical brief…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!d.technicalBrief.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SectionCard("Technical Brief", d.technicalBrief!!)
                }

                if (!d.competitiveLandscape.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SectionCard("Competes With", d.competitiveLandscape!!.joinToString(", "))
                }

                if (!d.riskFactors.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SectionCard("Risk Factors", d.riskFactors!!.joinToString("\n• ", prefix = "• "))
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.generateBlueprint(d.id, d.techniqueName) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Navy10,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating…", color = Navy10, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Description, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Product Blueprint", color = Navy10, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.generatePipeline(d.techniqueName, d.description) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleSecondary),
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Terminal, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Launch ML Training Pipeline", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(32.dp))
            } else {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Trend not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ScoreChip(
    modifier: Modifier = Modifier,
    label: String,
    value: Double,
    color: androidx.compose.ui.graphics.Color,
    displayText: String? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                displayText ?: "${"%.0f".format(value * 100)}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
fun SectionCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
            Spacer(Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

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

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isBriefLoading = MutableStateFlow(false)
    val isBriefLoading: StateFlow<Boolean> = _isBriefLoading

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
