package com.vectorminds.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.core.network.IngestRequest
import com.vectorminds.core.network.VectorMindsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalPapers: Int = 0,
    val totalRepos: Int = 0,
    val activeTrends: Int = 0,
    val blueprints: Int = 0,
    val pipelines: Int = 0,
    val avgNovelty: Double = 0.0,
    val isBackendOnline: Boolean = false,
    val isIngesting: Boolean = false,
    val lastIngestionResult: String = "",
    val galaxyPoints: List<com.vectorminds.core.network.VectorPoint> = emptyList(),
    val agentLogs: List<com.vectorminds.core.data.db.entity.ActionLogEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: com.vectorminds.core.network.VectorMindsApi,
    private val actionLogDao: com.vectorminds.core.data.db.dao.ActionLogDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        loadGalaxy()
        observeLogs()
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = api.getStats()
                _uiState.value = _uiState.value.copy(
                    totalPapers = stats.totalPapers,
                    totalRepos = stats.totalGithubRepos,
                    activeTrends = stats.activeTrends,
                    blueprints = stats.blueprintsGenerated,
                    pipelines = stats.pipelinesLaunched,
                    avgNovelty = stats.avgNoveltyScore,
                    isBackendOnline = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBackendOnline = false)
            }
        }
    }

    fun triggerIngestion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isIngesting = true, lastIngestionResult = "")
            try {
                val result = api.triggerIngestion(IngestRequest(source = "all"))
                _uiState.value = _uiState.value.copy(
                    isIngesting = false,
                    lastIngestionResult = "✅ Ingested ${result.signalsIngested} signals, ${result.trendsUpdated} trends updated",
                )
                loadStats() // Refresh stats
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isIngesting = false,
                    lastIngestionResult = "❌ Ingestion failed: ${e.message}",
                )
            }
        }
    fun loadGalaxy() {
        viewModelScope.launch {
            try {
                val response = api.getVectorMap(limit = 100)
                _uiState.value = _uiState.value.copy(galaxyPoints = response.points)
            } catch (_: Exception) { }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            actionLogDao.getAll().collect { logs ->
                _uiState.value = _uiState.value.copy(agentLogs = logs.take(10))
            }
        }
    }
}
