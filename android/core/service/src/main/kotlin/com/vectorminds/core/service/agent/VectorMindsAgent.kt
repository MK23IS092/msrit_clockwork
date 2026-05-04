package com.vectorminds.core.service.agent

import android.util.Log
import com.vectorminds.core.data.db.dao.ActionLogDao
import com.vectorminds.core.data.db.entity.ActionLogEntity
import com.vectorminds.core.network.VectorMindsApi
import com.vectorminds.core.skills.PlatformState
import com.vectorminds.core.skills.SkillRegistry
import com.vectorminds.core.skills.SkillResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The central VectorMind agent orchestrator.
 * Follows the same OpenClaw pattern as the central VectorMindAgent.
 */
@Singleton
class VectorMindAgent @Inject constructor(
    private val api: VectorMindsApi,
    private val skillRegistry: SkillRegistry,
    private val actionLogDao: ActionLogDao,
) {
    private var cycleCount = 0L

    suspend fun runCycle() {
        try {
            withTimeout(CYCLE_TIMEOUT_MS) {
                executeCycle()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Agent cycle exceeded ${CYCLE_TIMEOUT_MS}ms budget")
        } catch (e: Exception) {
            Log.e(TAG, "Agent cycle failed unexpectedly", e)
        }
    }

    private suspend fun executeCycle() {
        cycleCount++
        Log.i(TAG, "▶ VectorMind Cycle $cycleCount started")

        val state = buildPlatformState()

        val triggeredSkills = skillRegistry.skills.filter { it.shouldTrigger(state) }

        for (skill in triggeredSkills) {
            val result = try {
                skill.execute(state)
            } catch (e: Exception) {
                SkillResult.Failure("Error: ${e.message}")
            }

            // Log the result with description and reasoning
            val description = when (result) {
                is SkillResult.Success -> result.description
                is SkillResult.Failure -> "Failed: ${result.error}"
            }
            
            val logEntry = ActionLogEntity(
                id = UUID.randomUUID().toString(),
                skillId = skill.id,
                description = description,
                status = if (result is SkillResult.Success) "success" else "failed"
            )
            actionLogDao.insert(logEntry)
        }
    }

    private suspend fun buildPlatformState(): PlatformState {
        return try {
            val stats = api.getStats()
            PlatformState(
                totalSignals = stats.totalSignals,
                activeTrends = stats.activeTrends,
                blueprintsGenerated = stats.blueprintsGenerated,
                pipelinesLaunched = stats.pipelinesLaunched,
                isBackendReachable = true
            )
        } catch (e: Exception) {
            PlatformState(isBackendReachable = false)
        }
    }

    companion object {
        private const val TAG = "VectorMindAgent"
        private const val CYCLE_TIMEOUT_MS = 30_000L
    }
}
