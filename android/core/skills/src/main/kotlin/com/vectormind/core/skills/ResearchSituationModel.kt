package com.vectorminds.core.skills

/**
 * Situational model for research intelligence.
 * Inspired by the VectorMind SituationModel.
 */
data class ResearchSituationModel(
    val currentTime: Long,
    val location: ResearchLocation = ResearchLocation.UNKNOWN,
    val nextMeeting: MeetingContext? = null,
    val activeFocus: String? = null, // e.g., "Transformers", "Generative AI"
    val isBackendReachable: Boolean = false,
    
    // Platform stats (synced from backend)
    val totalSignals: Int = 0,
    val activeTrends: Int = 0,
    val topTrendScore: Double = 0.0
)

enum class ResearchLocation {
    HOME, LAB, OFFICE, CONFERENCE, UNKNOWN
}

data class MeetingContext(
    val title: String,
    val attendeeNames: List<String>,
    val startTime: Long,
    val isVirtual: Boolean,
    val meetingLink: String? = null
)
