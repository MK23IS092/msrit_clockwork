package com.vectorminds.app.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectorminds.core.network.TrendItem
import com.vectorminds.core.network.VectorMindsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendUiState(
    val trends: List<TrendItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val api: VectorMindsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendUiState())
    val uiState: StateFlow<TrendUiState> = _uiState.asStateFlow()

    init {
        loadTrends()
    }

    fun loadTrends() {
        viewModelScope.launch {
            _uiState.value = TrendUiState(isLoading = true)
            try {
                val response = api.getTrends(limit = 20)
                _uiState.value = TrendUiState(trends = response.trends, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = TrendUiState(isLoading = false, error = e.message)
            }
        }
    fun searchTrends(query: String) {
        if (query.length < 3) {
            if (query.isEmpty()) loadTrends()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = api.semanticSearch(com.vectorminds.core.network.SearchRequest(query = query))
                // Convert search results to TrendItems (simplified for demo)
                val results = response.results.map { res ->
                    TrendItem(
                        id = res.id,
                        rank = 0,
                        techniqueName = (res.payload?.get("title") as? String) ?: "Search Result",
                        description = (res.payload?.get("summary") as? String) ?: "",
                        emergenceScore = res.score,
                        noveltyScore = (res.payload?.get("novelty_score") as? Double) ?: 0.0,
                        impactScore = res.score,
                        mainstreamEtaMonths = 12,
                        confidence = res.score,
                        sourceSignals = null,
                        competitiveLandscape = null,
                        riskFactors = null,
                        relatedTechniques = null,
                        paperCount = 1,
                        githubStars = 0
                    )
                }
                _uiState.value = _uiState.value.copy(trends = results, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
