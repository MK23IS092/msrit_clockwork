package com.vectorminds.core.skills

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry holding all available [Skill] implementations, provided by Hilt.
 * Skills are registered via a Hilt multi-binding set.
 */
@Singleton
class SkillRegistry @Inject constructor(
    val skills: Set<@JvmSuppressWildcards Skill>,
) {
    fun findById(id: String): Skill? = skills.find { it.id == id }
}
