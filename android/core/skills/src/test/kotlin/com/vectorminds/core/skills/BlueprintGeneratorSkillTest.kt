package com.vectorminds.core.skills

import com.vectorminds.core.network.BlueprintResponse
import com.vectorminds.core.network.TrendItem
import com.vectorminds.core.network.TrendsResponse
import com.vectorminds.core.network.VectorMindsApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintGeneratorSkillTest {

    @Test
    fun `execute returns skipped when no trends`() = runTest {
        val api = mockk<VectorMindsApi>()
        coEvery { api.getTrends(1) } returns TrendsResponse(trends = emptyList(), count = 0)
        val skill = BlueprintGeneratorSkill(api)

        val result = skill.execute(PlatformState(isBackendReachable = true, activeTrends = 1, topTrendScore = 0.7))

        assertTrue(result is SkillResult.Skipped)
    }

    @Test
    fun `execute generates blueprint for top trend`() = runTest {
        val api = mockk<VectorMindsApi>()
        val trend = TrendItem(
            id = "trend-1",
            rank = 1,
            techniqueName = "Sparse Attention",
            description = "desc",
            emergenceScore = 0.8,
            noveltyScore = 0.7,
            impactScore = 0.75,
            mainstreamEtaMonths = 9,
            confidence = 0.9,
            sourceSignals = null,
            competitiveLandscape = null,
            riskFactors = null,
            relatedTechniques = null,
            paperCount = 2,
            githubStars = 50,
        )
        coEvery { api.getTrends(1) } returns TrendsResponse(trends = listOf(trend), count = 1)
        coEvery { api.generateBlueprint(any()) } returns BlueprintResponse(
            id = "bp-1",
            techniqueName = "Sparse Attention",
            trendId = "trend-1",
            problemStatement = "problem",
            marketSize = "large",
            technicalImplementation = "impl",
            architectureDecisions = listOf("a"),
            differentiationStrategy = "d",
            datasetRequirements = "data",
            goToMarket = "gtm",
            riskAssessment = "risk",
            first90DayMilestones = listOf("m1"),
            suggestedStack = listOf("kotlin"),
        )
        val skill = BlueprintGeneratorSkill(api)

        val result = skill.execute(PlatformState(isBackendReachable = true, activeTrends = 1, topTrendScore = 0.9))

        assertTrue(result is SkillResult.Success)
    }
}
