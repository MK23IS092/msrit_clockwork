package com.vectorminds.core.skills

import com.vectorminds.core.network.IngestResponse
import com.vectorminds.core.network.VectorMindsApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendMonitorSkillTest {

    @Test
    fun `execute returns success when ingestion succeeds`() = runTest {
        val api = mockk<VectorMindsApi>()
        coEvery { api.triggerIngestion(any()) } returns IngestResponse(
            status = "success",
            signalsIngested = 12,
            trendsUpdated = 4,
        )
        val skill = TrendMonitorSkill(api)

        val result = skill.execute(PlatformState(isBackendReachable = true))

        assertTrue(result is SkillResult.Success)
        val msg = (result as SkillResult.Success).description
        assertTrue(msg.contains("Ingested 12"))
    }

    @Test
    fun `shouldTrigger returns true when stale or empty`() {
        val api = mockk<VectorMindsApi>(relaxed = true)
        val skill = TrendMonitorSkill(api)

        assertTrue(
            skill.shouldTrigger(
                PlatformState(
                    isBackendReachable = true,
                    totalSignals = 0,
                    minutesSinceLastIngestion = 5,
                )
            )
        )
        assertTrue(
            skill.shouldTrigger(
                PlatformState(
                    isBackendReachable = true,
                    totalSignals = 10,
                    minutesSinceLastIngestion = 61,
                )
            )
        )
    }
}
