package com.vectorminds.app.ui.blueprints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.app.ui.common.OnResumeEffect
import com.vectorminds.app.ui.theme.*
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

    OnResumeEffect { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Product Blueprints", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Startup-ready product briefs from emerging techniques", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, "Refresh blueprints", tint = CyanPrimary)
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading && blueprints.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            }
            blueprints.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (error != null) "Couldn't load blueprints" else "No blueprints yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            error ?: "Open a trend and tap \"Generate Product Blueprint\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(blueprints) { bp ->
                        BlueprintCard(bp)
                    }
                }
            }
        }
    }
}

@Composable
fun BlueprintCard(bp: BlueprintResponse) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(bp.techniqueName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
            Text("Market: ${bp.marketSize}", style = MaterialTheme.typography.bodySmall, color = EmeraldTertiary)

            Spacer(Modifier.height(8.dp))
            Text(bp.problemStatement.take(150) + if (bp.problemStatement.length > 150) "..." else "", style = MaterialTheme.typography.bodySmall)

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))

                SectionText("Technical Implementation", bp.technicalImplementation)
                SectionText("Differentiation", bp.differentiationStrategy)
                SectionText("Go-to-Market", bp.goToMarket)
                SectionText("Risk Assessment", bp.riskAssessment)

                if (bp.suggestedStack.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Stack", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PurpleSecondary)
                    bp.suggestedStack.forEach { tech ->
                        Text("• $tech", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionText(title: String, content: String) {
    if (content.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PurpleSecondary)
        Text(content, style = MaterialTheme.typography.bodySmall)
    }
}

@HiltViewModel
class BlueprintViewModel @Inject constructor(private val api: VectorMindsApi) : ViewModel() {
    private val _blueprints = MutableStateFlow<List<BlueprintResponse>>(emptyList())
    val blueprints: StateFlow<List<BlueprintResponse>> = _blueprints

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
    }

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
