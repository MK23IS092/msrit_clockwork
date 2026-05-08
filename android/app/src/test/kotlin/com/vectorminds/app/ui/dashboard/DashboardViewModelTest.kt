package com.vectorminds.app.ui.dashboard

import com.vectorminds.app.testing.MainDispatcherRule
import com.vectorminds.core.data.db.dao.ActionLogDao
import com.vectorminds.core.data.db.entity.ActionLogEntity
import com.vectorminds.core.network.DashboardPremiumContextResponse
import com.vectorminds.core.network.IngestResponse
import com.vectorminds.core.network.IngestionStatus
import com.vectorminds.core.network.StatsResponse
import com.vectorminds.core.network.VectorMapResponse
import com.vectorminds.core.network.VectorMindsApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `triggerIngestion updates status and summary message`() = runTest(mainDispatcherRule.dispatcher) {
        val api = mockk<VectorMindsApi>()
        val dao = mockk<ActionLogDao>()

        coEvery { api.getStats() } returns StatsResponse(
            totalSignals = 10,
            totalPapers = 4,
            totalGithubRepos = 6,
            activeTrends = 2,
            blueprintsGenerated = 1,
            pipelinesLaunched = 1,
            avgNoveltyScore = 0.66,
            noveltyDistribution = listOf(0.6, 0.7),
            agentsStatus = mapOf("ingestion" to "running"),
            lastUpdated = "now",
        )
        coEvery { api.getVectorMap(40) } returns VectorMapResponse(points = emptyList(), count = 0, explainedVariance = null)
        coEvery { api.getDashboardPremiumContext() } returns DashboardPremiumContextResponse(
            location = "",
            focus = "",
            nextMeeting = "",
            authorName = "",
            papersCount = 0,
            confidence = 0.0,
            reasoningPoints = emptyList(),
            sourceModes = emptyMap(),
        )
        coEvery { api.triggerIngestion(any()) } returns IngestResponse(
            status = "started",
            signalsIngested = null,
            trendsUpdated = null,
        )
        coEvery { api.getIngestionStatus() } returnsMany listOf(
            IngestionStatus(state = "running"),
            IngestionStatus(
                state = "completed",
                signalsIngested = 7,
                trendsUpdated = 3,
            ),
        )
        every { dao.getAll() } returns flowOf(
            listOf(ActionLogEntity(id = "1", skillId = "s1", description = "ok", status = "success"))
        )

        val vm = DashboardViewModel(api, dao)
        advanceUntilIdle()
        vm.triggerIngestion()
        advanceUntilIdle()
        advanceTimeBy(3_000)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.lastIngestionResult.contains("Ingested 7 signals"))
        assertEquals(4, vm.uiState.value.totalPapers)
        assertEquals(6, vm.uiState.value.totalRepos)
    }
}
