package com.vectorminds.core.skills

/**
 * Result sealed class returned by every Skill.execute() call.
 * Follows the standard VectorMind SkillResult pattern.
 */
sealed class SkillResult {
    data class Success(val message: String) : SkillResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : SkillResult()
    data object Skipped : SkillResult()
}
