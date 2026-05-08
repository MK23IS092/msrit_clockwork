package com.vectorminds.core.skills

import android.util.Log
import com.vectorminds.core.network.VectorMindsApi
import javax.inject.Inject

/**
 * Monitors for exceptionally high-impact trends and triggers local alerts.
 * This complements the backend's Telegram notifications with on-device system notifications.
 */
class AlertSkill @Inject constructor(
    private val api: VectorMindsApi,
) : Skill {

    override val id = "high_impact_alert"
    override val name = "Intelligence Alert"
    override val description = "Surfaces critical intelligence alerts for breakthrough techniques"

    override fun shouldTrigger(state: PlatformState): Boolean {
        // Trigger if a trend has a very high emergence score (> 0.8)
        return state.isBackendReachable && state.topTrendScore > 0.8
    }

    override suspend fun execute(state: PlatformState): SkillResult {
        return try {
            val trends = api.getTrends(limit = 1)
            if (trends.trends.isEmpty()) return SkillResult.Skipped
            
            val topTrend = trends.trends.first()
            if (topTrend.emergenceScore > 0.8) {
                Log.i(TAG, "High impact trend detected: ${topTrend.techniqueName}")
                SkillResult.Success(
                    "CRITICAL: Breakthrough technique '${topTrend.techniqueName}' detected " +
                    "with ${"%.0f".format(topTrend.emergenceScore * 100)}% emergence score!"
                )
            } else {
                SkillResult.Skipped
            }
        } catch (e: Exception) {
            Log.e(TAG, "Alert check failed", e)
            SkillResult.Failure("Alert check failed: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "AlertSkill"
    }
}
