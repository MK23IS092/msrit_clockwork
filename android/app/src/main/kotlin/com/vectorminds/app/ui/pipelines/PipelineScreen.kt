package com.vectorminds.app.ui.pipelines

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
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
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Text("ML Pipelines", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Auto-generated training pipelines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    init { viewModelScope.launch { try { _pipelines.value = api.listPipelines().pipelines } catch (_: Exception) { }; _isLoading.value = false } }
}
