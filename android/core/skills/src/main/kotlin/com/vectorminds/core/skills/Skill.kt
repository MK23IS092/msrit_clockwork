package com.vectorminds.core.skills

/**
 * Base interface that every VectorMind skill must implement.
 * Follows the OpenClaw Skill contract for VectorMind.
 */
interface Skill {
    val id: String
    val name: String
    val description: String

    fun shouldTrigger(state: PlatformState): Boolean
    suspend fun execute(state: PlatformState): SkillResult
}

/**
 * Platform state passed to skills on every agent cycle.
 */
data class PlatformState(
    val totalSignals: Int = 0,
    val activeTrends: Int = 0,
    val lastIngestionTimestamp: Long = 0L,
    val topTrendScore: Double = 0.0,
    val blueprintsGenerated: Int = 0,
    val pipelinesLaunched: Int = 0,
    val isBackendReachable: Boolean = false,
    val minutesSinceLastIngestion: Long = Long.MAX_VALUE,
)
