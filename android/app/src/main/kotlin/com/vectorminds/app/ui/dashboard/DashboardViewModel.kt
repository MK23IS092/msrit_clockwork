package com.vectorminds.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.core.network.IngestRequest
import com.vectorminds.core.network.VectorMindsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
    val situationLocation: String = "Loading...",
    val situationFocus: String = "Loading...",
    val situationMeeting: String = "Loading...",
    val authorName: String = "Loading...",
    val authorPapersCount: Int = 0,
    val reasoningConfidence: Float = 0.0f,
    val reasoningPoints: List<String> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: com.vectorminds.core.network.VectorMindsApi,
    private val actionLogDao: com.vectorminds.core.data.db.dao.ActionLogDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
        refreshDashboardParallel()
    }

    /**
     * Stats, premium context, and vector map in parallel — cuts cold-start
     * wait from ~3× slowest call to ~1× slowest call.
     */
    private fun refreshDashboardParallel() {
        viewModelScope.launch {
            supervisorScope {
                val statsD = async { runCatching { api.getStats() }.getOrNull() }
                val ctxD = async { runCatching { api.getDashboardPremiumContext() }.getOrNull() }
                val mapD = async { runCatching { api.getVectorMap(limit = 40) }.getOrNull() }

                val stats = statsD.await()
                val ctx = ctxD.await()
                val map = mapD.await()

                var s = _uiState.value
                if (stats != null) {
                    s = s.copy(
                        totalPapers = stats.totalPapers,
                        totalRepos = stats.totalGithubRepos,
                        activeTrends = stats.activeTrends,
                        blueprints = stats.blueprintsGenerated,
                        pipelines = stats.pipelinesLaunched,
                        avgNovelty = stats.avgNoveltyScore,
                        isBackendOnline = true,
                    )
                } else {
                    s = s.copy(isBackendOnline = false)
                }
                if (ctx != null) {
                    s = s.copy(
                        situationLocation = ctx.location,
                        situationFocus = ctx.focus,
                        situationMeeting = ctx.nextMeeting,
                        authorName = ctx.authorName,
                        authorPapersCount = ctx.papersCount,
                        reasoningConfidence = ctx.confidence.toFloat(),
                        reasoningPoints = ctx.reasoningPoints,
                    )
                }
                if (map != null) {
                    s = s.copy(galaxyPoints = map.points)
                }
                _uiState.value = s
            }
        }
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
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isBackendOnline = false)
            }
        }
    }

    fun triggerIngestion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isIngesting = true, lastIngestionResult = "Starting ingestion...")
            try {
                api.triggerIngestion(IngestRequest(source = "all", background = true))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isIngesting = false,
                    lastIngestionResult = "Ingestion failed to start: ${e.message ?: "network error"}",
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                lastIngestionResult = "Ingestion running in the background. Polling for completion...",
            )

            val deadlineMs = System.currentTimeMillis() + 5 * 60 * 1000L
            while (System.currentTimeMillis() < deadlineMs) {
                kotlinx.coroutines.delay(3_000)
                val status = try {
                    api.getIngestionStatus()
                } catch (_: Exception) {
                    continue
                }
                when (status.state) {
                    "completed" -> {
                        _uiState.value = _uiState.value.copy(
                            isIngesting = false,
                            lastIngestionResult = "Ingested ${status.signalsIngested} signals, ${status.trendsUpdated} trends updated",
                        )
                        refreshDashboardParallel()
                        return@launch
                    }
                    "failed" -> {
                        _uiState.value = _uiState.value.copy(
                            isIngesting = false,
                            lastIngestionResult = "Ingestion failed: ${status.error ?: "unknown error"}",
                        )
                        return@launch
                    }
                    else -> { /* still running */ }
                }
            }
            _uiState.value = _uiState.value.copy(
                isIngesting = false,
                lastIngestionResult = "Ingestion still running on the server. Pull to refresh later.",
            )
        }
    }

    fun loadGalaxy() {
        viewModelScope.launch {
            try {
                val response = api.getVectorMap(limit = 40)
                _uiState.value = _uiState.value.copy(galaxyPoints = response.points)
            } catch (_: Exception) { }
        }
    }

    fun loadPremiumContext() {
        viewModelScope.launch {
            try {
                val context = api.getDashboardPremiumContext()
                _uiState.value = _uiState.value.copy(
                    situationLocation = context.location,
                    situationFocus = context.focus,
                    situationMeeting = context.nextMeeting,
                    authorName = context.authorName,
                    authorPapersCount = context.papersCount,
                    reasoningConfidence = context.confidence.toFloat(),
                    reasoningPoints = context.reasoningPoints,
                )
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
