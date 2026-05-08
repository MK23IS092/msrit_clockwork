package com.vectorminds.core.skills

import android.util.Log
import com.vectorminds.core.network.PipelineRequest
import com.vectorminds.core.network.VectorMindsApi
import javax.inject.Inject

/**
 * Automatically generates ML training pipelines for validated blueprints.
 * Triggers when blueprints are available but pipelines have not been launched.
 */
class PipelineLauncherSkill @Inject constructor(
    private val api: VectorMindsApi,
) : Skill {

    override val id = "pipeline_launcher"
    override val name = "Pipeline Launcher"
    override val description = "Auto-generates ML training pipelines for emerging techniques"

    override fun shouldTrigger(state: PlatformState): Boolean {
        // Trigger when there are blueprints but fewer pipelines
        return state.isBackendReachable &&
                state.blueprintsGenerated > 0 &&
                state.pipelinesLaunched < state.blueprintsGenerated
    }

    override suspend fun execute(state: PlatformState): SkillResult {
        return try {
            // Get list of blueprints and generate pipeline for the latest one
            val blueprints = api.listBlueprints().blueprints
            if (blueprints.isEmpty()) return SkillResult.Skipped

            val latestBp = blueprints.last()
            val pipeline = api.generatePipeline(
                PipelineRequest(
                    techniqueName = latestBp.techniqueName,
                    description = latestBp.problemStatement
                )
            )
            Log.i(TAG, "Pipeline generated for: ${pipeline.techniqueName}")
            SkillResult.Success(
                "Generated training pipeline for '${pipeline.techniqueName}' — " +
                "Target: ${pipeline.modelArchitecture}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline generation failed", e)
            SkillResult.Failure("Pipeline generation failed: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "PipelineLauncherSkill"
    }
}
