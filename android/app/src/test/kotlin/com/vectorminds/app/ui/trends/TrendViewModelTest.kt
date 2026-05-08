package com.vectorminds.app.ui.trends

import com.vectorminds.app.testing.MainDispatcherRule
import com.vectorminds.core.network.SearchResponse
import com.vectorminds.core.network.TrendItem
import com.vectorminds.core.network.TrendsResponse
import com.vectorminds.core.network.VectorMindsApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrendViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadTrends populates ui state`() = runTest {
        val api = mockk<VectorMindsApi>()
        val trend = TrendItem(
            id = "t1",
            rank = 1,
            techniqueName = "Mamba",
            description = "State space models",
            emergenceScore = 0.8,
            noveltyScore = 0.7,
            impactScore = 0.75,
            mainstreamEtaMonths = 8,
            confidence = 0.9,
            sourceSignals = null,
            competitiveLandscape = null,
            riskFactors = null,
            relatedTechniques = null,
            paperCount = 3,
            githubStars = 100,
        )
        coEvery { api.getTrends(20) } returns TrendsResponse(listOf(trend), 1)
        coEvery { api.semanticSearch(any()) } returns SearchResponse(emptyList(), 0, "")

        val vm = TrendViewModel(api)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(1, vm.uiState.value.trends.size)
        assertEquals("Mamba", vm.uiState.value.trends.first().techniqueName)
    }
}
