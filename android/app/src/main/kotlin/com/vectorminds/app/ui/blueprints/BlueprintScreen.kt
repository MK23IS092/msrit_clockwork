package com.vectorminds.app.ui.blueprints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("Product Blueprints", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Startup-ready product briefs from emerging techniques", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (blueprints.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No blueprints yet", style = MaterialTheme.typography.titleMedium)
                    Text("Generate one from a trend's detail page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(blueprints) { bp ->
                    BlueprintCard(bp)
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
            Text(bp.problemStatement.take(150) + "...", style = MaterialTheme.typography.bodySmall)

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

    init {
        viewModelScope.launch {
            try {
                _blueprints.value = api.listBlueprints().blueprints
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }
}
