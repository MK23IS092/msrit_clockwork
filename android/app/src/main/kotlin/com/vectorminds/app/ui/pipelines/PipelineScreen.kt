package com.vectorminds.app.ui.pipelines

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.app.ui.common.OnResumeEffect
import com.vectorminds.core.network.DatasetCandidate
import com.vectorminds.core.network.DatasetCandidatesRequest
import com.vectorminds.core.network.PipelineRequest
import com.vectorminds.app.ui.theme.*
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
    var techniqueName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    OnResumeEffect { viewModel.refreshPipelines() }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ML Pipelines", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Auto-generated training pipelines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.refreshPipelines() }) {
                Icon(Icons.Default.Refresh, "Refresh pipelines", tint = CyanPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = techniqueName,
            onValueChange = { techniqueName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Technique") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description (optional)") },
            maxLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.fetchDatasetCandidates(techniqueName, description) },
                enabled = techniqueName.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Preview Datasets") }
            Button(
                onClick = { viewModel.generatePipeline(techniqueName, description) },
                enabled = techniqueName.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Generate Pipeline") }
        }
        if (status.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(status, style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
        }
        if (candidates.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Dataset Candidates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(candidates) { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.generatePipeline(techniqueName, description) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${c.source} • downloads: ${c.downloads}", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CyanPrimary) }
        } else if (pipelines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Terminal, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No pipelines yet", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pipelines) { pl ->
                    val context = LocalContext.current
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(pl.techniqueName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Task: ${pl.taskType} • Model: ${pl.modelArchitecture}", style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pl.colabUrl))) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)) {
                                Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(8.dp)); Text("Open in Google Colab")
                            }
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class PipelineViewModel @Inject constructor(private val api: VectorMindsApi) : ViewModel() {
    private val _pipelines = MutableStateFlow<List<PipelineResponse>>(emptyList())
    val pipelines: StateFlow<List<PipelineResponse>> = _pipelines
    private val _candidates = MutableStateFlow<List<DatasetCandidate>>(emptyList())
    val candidates: StateFlow<List<DatasetCandidate>> = _candidates
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    init {
        refreshPipelines()
    }

    fun fetchDatasetCandidates(techniqueName: String, description: String) {
        viewModelScope.launch {
            if (techniqueName.isBlank()) return@launch
            _status.value = "Fetching candidates..."
            try {
                val resp = api.getDatasetCandidates(
                    DatasetCandidatesRequest(techniqueName = techniqueName, description = description)
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
            _status.value = "Generating pipeline..."
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
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
