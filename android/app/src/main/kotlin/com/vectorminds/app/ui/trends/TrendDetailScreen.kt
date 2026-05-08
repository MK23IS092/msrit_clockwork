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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun TrendDetailScreen(
    trendId: String,
    onBack: () -> Unit,
    viewModel: TrendDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(trendId) { viewModel.loadTrend(trendId) }
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back")
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (detail != null) {
            val d = detail!!

            Text(d.techniqueName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Score cards row
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

            // Description
            SectionCard("Description", d.description)

            // Technical Brief
            if (!d.technicalBrief.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard("Technical Brief", d.technicalBrief!!)
            }

            // Competitive Landscape
            if (!d.competitiveLandscape.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard("Competes With", d.competitiveLandscape!!.joinToString(", "))
            }

            // Risk Factors
            if (!d.riskFactors.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard("Risk Factors", d.riskFactors!!.joinToString("\n• ", prefix = "• "))
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = { viewModel.generateBlueprint(d.id) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Description, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Product Blueprint", color = Navy10, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.generatePipeline(d.techniqueName, d.description) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleSecondary),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Terminal, null)
                Spacer(Modifier.width(8.dp))
                Text("Launch ML Training Pipeline", fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(32.dp))
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

@HiltViewModel
class TrendDetailViewModel @Inject constructor(
    private val api: VectorMindsApi,
) : ViewModel() {
    private val _detail = MutableStateFlow<TrendDetail?>(null)
    val detail: StateFlow<TrendDetail?> = _detail

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadTrend(trendId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _detail.value = api.getTrendDetail(trendId)
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    fun generateBlueprint(trendId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.generateBlueprint(com.vectorminds.core.network.BlueprintRequest(trendId = trendId))
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    fun generatePipeline(name: String, desc: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.generatePipeline(com.vectorminds.core.network.PipelineRequest(techniqueName = name, description = desc))
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }
}
