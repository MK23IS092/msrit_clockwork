package com.vectorminds.core.skills

import android.util.Log
import com.vectorminds.core.network.VectorMindsApi
import javax.inject.Inject

/**
 * Monitors research trends and triggers ingestion when data is stale.
 * Periodically fetches new papers from arXiv and GitHub via the backend.
 */
class TrendMonitorSkill @Inject constructor(
    private val api: VectorMindsApi,
) : Skill {

    override val id = "trend_monitor"
    override val name = "Trend Monitor"
    override val description = "Automatically ingests new research papers and monitors emerging AI techniques"

    override fun shouldTrigger(state: PlatformState): Boolean {
        // Trigger if no data yet, or data is older than 60 minutes
        return state.isBackendReachable &&
                (state.totalSignals == 0 || state.minutesSinceLastIngestion > 60)
    }

    override suspend fun execute(state: PlatformState): SkillResult {
        return try {
            val response = api.triggerIngestion(
                com.vectorminds.core.network.IngestRequest(source = "all")
            )
            Log.i(TAG, "Ingestion complete: ${response.signalsIngested} signals")
            SkillResult.Success(
                "Ingested ${response.signalsIngested} new research signals, " +
                "${response.trendsUpdated} trends updated"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ingestion failed", e)
            SkillResult.Failure("Ingestion failed: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "TrendMonitorSkill"
    }
}
