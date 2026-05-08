package com.vectorminds.core.skills

import javax.inject.Inject

/**
 * Skill that provides context before meetings with researchers/authors.
 */
class AuthorContextSkill @Inject constructor() : Skill {
    override val id: String = "author_context_preparer"
    override val name: String = "Author Intelligence"
    override val description: String = "Prepares technical briefs before researcher meetings."

    override fun shouldTrigger(state: PlatformState): Boolean {
        // Simplified check: if backend is reachable and we have any meeting
        // In full impl, this would check if attendees match known authors
        return state.isBackendReachable && state.minutesSinceLastIngestion < 60
    }

    override suspend fun execute(state: PlatformState): SkillResult {
        // Mocking the 'finding an author' logic
        val reasoning = listOf(
            "Meeting 'Paper Review' starts in 10 mins",
            "Attendee 'Dr. Arxiv' is a top author in Graph Neural Networks",
            "3 related papers found in recent ingestion"
        )
        
        return SkillResult.Success(
            description = "Compiled a technical brief for your meeting with Dr. Arxiv.",
            reasoning = reasoning
        )
    }
}
