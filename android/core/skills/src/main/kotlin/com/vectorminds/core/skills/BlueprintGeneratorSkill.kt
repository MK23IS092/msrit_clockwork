package com.vectorminds.core.skills

import android.util.Log
import com.vectorminds.core.network.BlueprintRequest
import com.vectorminds.core.network.VectorMindsApi
import javax.inject.Inject

/**
 * Automatically generates product blueprints for high-scoring trends.
 * Triggers when a trend has a high emergence score but no blueprint yet.
 */
class BlueprintGeneratorSkill @Inject constructor(
    private val api: VectorMindsApi,
) : Skill {

    override val id = "blueprint_generator"
    override val name = "Blueprint Generator"
    override val description = "Auto-generates product blueprints for top emerging AI techniques"

    override fun shouldTrigger(state: PlatformState): Boolean {
        // Trigger when there are active trends but fewer blueprints
        return state.isBackendReachable &&
                state.activeTrends > 0 &&
                state.blueprintsGenerated < state.activeTrends &&
                state.topTrendScore > 0.5
    }

    override suspend fun execute(state: PlatformState): SkillResult {
        return try {
            // Get top trend and generate blueprint for it
            val trends = api.getTrends(limit = 1)
            if (trends.trends.isEmpty()) return SkillResult.Skipped

            val topTrend = trends.trends.first()
            val blueprint = api.generateBlueprint(
                BlueprintRequest(trendId = topTrend.id)
            )
            Log.i(TAG, "Blueprint generated for: ${blueprint.techniqueName}")
            SkillResult.Success(
                "Generated product blueprint for '${blueprint.techniqueName}' — " +
                "market size: ${blueprint.marketSize}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Blueprint generation failed", e)
            SkillResult.Failure("Blueprint generation failed: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "BlueprintGenSkill"
    }
}
