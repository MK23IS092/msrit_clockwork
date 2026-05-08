package com.vectorminds.app.ui.pipelines

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.app.ui.common.OnResumeEffect
import com.vectorminds.app.ui.components.VmButton
import com.vectorminds.app.ui.components.VmButtonStyle
import com.vectorminds.app.ui.components.VmCard
import com.vectorminds.app.ui.components.VmChip
import com.vectorminds.app.ui.components.VmChipStyle
import com.vectorminds.app.ui.components.VmEmptyState
import com.vectorminds.app.ui.components.VmScreenHeader
import com.vectorminds.app.ui.components.VmScreenSurface
import com.vectorminds.app.ui.components.VmShimmerBox
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.core.network.DatasetCandidate
import com.vectorminds.core.network.DatasetCandidatesRequest
import com.vectorminds.core.network.PipelineRequest
import com.vectorminds.core.network.PipelineResponse
import com.vectorminds.core.network.VectorMindsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun PipelineScreen(viewModel: PipelineViewModel = hiltViewModel()) {
    val pipelines by viewModel.pipelines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val candidates by viewModel.candidates.collectAsState()
    val status by viewModel.status.collectAsState()
    val vm = Vm.colors
    var techniqueName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    OnResumeEffect { viewModel.refreshPipelines() }

    VmScreenSurface {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                VmScreenHeader(
                    eyebrow = "MLOps",
                    title = "Training Pipelines",
                    subtitle = "Auto-generated notebooks ready for Colab",
                    trailing = {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(vm.surfaceElevated)
                                .border(1.dp, vm.border, RoundedCornerShape(12.dp))
                                .clickable { viewModel.refreshPipelines() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = vm.textMuted, modifier = Modifier.size(16.dp))
                        }
                    },
                )
            }

            // ── Generate-new card ─────────────────────────────────────────
            item {
                VmCard {
                    Text(
                        "GENERATE NEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = vm.brand,
                    )
                    Spacer(Modifier.height(12.dp))

                    InlineLabel("Technique")
                    Spacer(Modifier.height(6.dp))
                    InlineField(
                        value = techniqueName,
                        onValueChange = { techniqueName = it },
                        placeholder = "e.g. Sparse Mixture of Experts",
                    )

                    Spacer(Modifier.height(12.dp))
                    InlineLabel("Description")
                    Spacer(Modifier.height(6.dp))
                    InlineField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Short technical context (optional)",
                        minHeight = 64.dp,
                    )

                    Spacer(Modifier.height(16.dp))
                    // Vertical stack — no two-column wrap on narrow phones.
                    VmButton(
                        text = "Generate Pipeline",
                        onClick = { viewModel.generatePipeline(techniqueName, description) },
                        icon = Icons.Filled.PlayArrow,
                        enabled = techniqueName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    VmButton(
                        text = "Preview Dataset Candidates",
                        onClick = { viewModel.fetchDatasetCandidates(techniqueName, description) },
                        style = VmButtonStyle.Secondary,
                        icon = Icons.Filled.Storage,
                        enabled = techniqueName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            status,
                            style = MaterialTheme.typography.labelSmall,
                            color = vm.brand,
                        )
                    }

                    if (candidates.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        InlineLabel("Dataset candidates")
                        Spacer(Modifier.height(8.dp))
                        // Static Column — never nest a vertical LazyColumn
                        // inside another vertical LazyColumn (Compose throws).
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidates.take(6).forEach { c -> CandidateRow(c) }
                        }
                    }
                }
            }

            // ── Existing pipelines list ───────────────────────────────────
            when {
                isLoading && pipelines.isEmpty() -> items(3) { PipelineSkeleton() }
                pipelines.isEmpty() -> item {
                    VmEmptyState(
                        icon = Icons.Filled.Terminal,
                        title = "No pipelines yet",
                        subtitle = "Generate one above, or open a trend and tap “Launch ML Training Pipeline”.",
                    )
                }
                else -> items(pipelines, key = { it.id }) { pl -> PipelineCard(pl) }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PipelineCard(pl: PipelineResponse) {
    val vm = Vm.colors
    val context = LocalContext.current
    val statusColor = when (pl.status) {
        "completed" -> vm.success
        "failed" -> vm.danger
        "training" -> vm.amber
        else -> vm.brand
    }

    VmCard(accent = statusColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Terminal, null, tint = statusColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pl.techniqueName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = vm.text,
                    maxLines = 2,
                )
                if (pl.modelArchitecture.isNotBlank()) {
                    Text(
                        pl.modelArchitecture,
                        style = MaterialTheme.typography.bodySmall,
                        color = vm.textMuted,
                        maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            VmChip(
                text = pl.status.replaceFirstChar { it.uppercase() },
                tint = statusColor,
                style = VmChipStyle.Soft,
            )
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (pl.taskType.isNotBlank()) {
                VmChip(text = pl.taskType, tint = vm.violet, style = VmChipStyle.Outline)
            }
            if (pl.datasetName.isNotBlank()) {
                VmChip(text = pl.datasetName, tint = vm.brand, style = VmChipStyle.Outline)
            }
        }
        Spacer(Modifier.height(14.dp))
        VmButton(
            text = "Open in Google Colab",
            onClick = {
                if (pl.colabUrl.isNotBlank()) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pl.colabUrl)))
                }
            },
            icon = Icons.Filled.OpenInBrowser,
            enabled = pl.colabUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PipelineSkeleton() {
    val vm = Vm.colors
    VmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(vm.surfaceElevated),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
                Spacer(Modifier.height(6.dp))
                VmShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp))
            }
        }
    }
}

@Composable
private fun CandidateRow(c: DatasetCandidate) {
    val vm = Vm.colors
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(vm.surfaceElevated)
            .border(1.dp, vm.border, RoundedCornerShape(10.dp))
            .clickable {
                if (c.url.isNotBlank()) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.url)))
                }
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(vm.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Storage, null, tint = vm.brand, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                c.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = vm.text,
                maxLines = 1,
            )
            Text(
                "${c.source} · ${c.downloads} downloads",
                style = MaterialTheme.typography.labelSmall,
                color = vm.textFaint,
            )
        }
    }
}

@Composable
private fun InlineLabel(text: String) {
    val vm = Vm.colors
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = vm.textFaint,
    )
}

@Composable
private fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val vm = Vm.colors
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(vm.surfaceElevated)
            .border(1.dp, vm.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
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
            singleLine = minHeight <= 44.dp,
            cursorBrush = SolidColor(vm.brand),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = vm.text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val api: VectorMindsApi,
) : ViewModel() {
    private val _pipelines = MutableStateFlow<List<PipelineResponse>>(emptyList())
    val pipelines: StateFlow<List<PipelineResponse>> = _pipelines

    private val _candidates = MutableStateFlow<List<DatasetCandidate>>(emptyList())
    val candidates: StateFlow<List<DatasetCandidate>> = _candidates

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    init { refreshPipelines() }

    fun fetchDatasetCandidates(techniqueName: String, description: String) {
        viewModelScope.launch {
            if (techniqueName.isBlank()) return@launch
            _status.value = "Fetching dataset candidates…"
            try {
                val resp = api.getDatasetCandidates(
                    DatasetCandidatesRequest(techniqueName = techniqueName, description = description),
                )
                _candidates.value = resp.candidates
                _status.value = "Found ${resp.count} candidates"
            } catch (e: Exception) {
                _status.value = "Candidate fetch failed: ${e.message}"
            }
        }
    }

    fun generatePipeline(techniqueName: String, description: String) {
        viewModelScope.launch {
            if (techniqueName.isBlank()) return@launch
            _status.value = "Generating pipeline…"
            try {
                api.generatePipeline(PipelineRequest(techniqueName = techniqueName, description = description))
                _status.value = "Pipeline generated"
                refreshPipelines()
            } catch (e: Exception) {
                _status.value = "Pipeline generation failed: ${e.message}"
            }
        }
    }

    fun refreshPipelines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _pipelines.value = api.listPipelines().pipelines
            } catch (_: Exception) { } finally {
                _isLoading.value = false
            }
        }
    }
}
