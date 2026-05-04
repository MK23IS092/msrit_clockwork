package com.vectorminds.core.network

import com.google.gson.annotations.SerializedName

// ─── Request Models ──────────────────────────────────────────

data class IngestRequest(
    val source: String = "all",
    val category: String? = null
)

data class BlueprintRequest(
    @SerializedName("trend_id") val trendId: String,
    @SerializedName("additional_context") val additionalContext: String = ""
)

data class PipelineRequest(
    @SerializedName("technique_name") val techniqueName: String,
    val description: String = "",
    @SerializedName("task_type") val taskType: String? = null
)

data class SearchRequest(
    val query: String,
    @SerializedName("top_k") val topK: Int = 10,
    @SerializedName("source_filter") val sourceFilter: String? = null
)

data class FeedbackRequest(
    @SerializedName("target_id") val targetId: String,
    @SerializedName("target_type") val targetType: String = "trend",
    val action: String = "upvote"
)

// ─── Response Models ─────────────────────────────────────────

data class HealthResponse(
    val status: String,
    val timestamp: String,
    val agents: Map<String, AgentHealth>?,
    @SerializedName("vector_store_count") val vectorStoreCount: Int
)

data class AgentHealth(
    val name: String,
    val status: String,
    val running: Boolean,
    @SerializedName("events_processed") val eventsProcessed: Int,
    @SerializedName("last_heartbeat") val lastHeartbeat: String
)

data class StatsResponse(
    @SerializedName("total_signals") val totalSignals: Int,
    @SerializedName("total_papers") val totalPapers: Int,
    @SerializedName("total_github_repos") val totalGithubRepos: Int,
    @SerializedName("active_trends") val activeTrends: Int,
    @SerializedName("blueprints_generated") val blueprintsGenerated: Int,
    @SerializedName("pipelines_launched") val pipelinesLaunched: Int,
    @SerializedName("avg_novelty_score") val avgNoveltyScore: Double,
    @SerializedName("novelty_distribution") val noveltyDistribution: List<Double>,
    @SerializedName("agents_status") val agentsStatus: Map<String, String>,
    @SerializedName("last_updated") val lastUpdated: String
)

data class TrendsResponse(
    val trends: List<TrendItem>,
    val count: Int
)

data class TrendItem(
    val id: String,
    val rank: Int,
    @SerializedName("technique_name") val techniqueName: String,
    val description: String,
    @SerializedName("emergence_score") val emergenceScore: Double,
    @SerializedName("novelty_score") val noveltyScore: Double,
    @SerializedName("impact_score") val impactScore: Double,
    @SerializedName("mainstream_eta_months") val mainstreamEtaMonths: Int,
    val confidence: Double,
    @SerializedName("source_signals") val sourceSignals: Map<String, Any>?,
    @SerializedName("competitive_landscape") val competitiveLandscape: List<String>?,
    @SerializedName("risk_factors") val riskFactors: List<String>?,
    @SerializedName("related_techniques") val relatedTechniques: List<String>?,
    @SerializedName("paper_count") val paperCount: Int,
    @SerializedName("github_stars") val githubStars: Int
)

data class TrendDetail(
    val id: String,
    val rank: Int,
    @SerializedName("technique_name") val techniqueName: String,
    val description: String,
    @SerializedName("emergence_score") val emergenceScore: Double,
    @SerializedName("novelty_score") val noveltyScore: Double,
    @SerializedName("impact_score") val impactScore: Double,
    @SerializedName("mainstream_eta_months") val mainstreamEtaMonths: Int,
    val confidence: Double,
    @SerializedName("source_signals") val sourceSignals: Map<String, Any>?,
    @SerializedName("competitive_landscape") val competitiveLandscape: List<String>?,
    @SerializedName("risk_factors") val riskFactors: List<String>?,
    @SerializedName("related_techniques") val relatedTechniques: List<String>?,
    @SerializedName("technical_brief") val technicalBrief: String?
)

data class BlueprintResponse(
    val id: String,
    @SerializedName("technique_name") val techniqueName: String,
    @SerializedName("trend_id") val trendId: String?,
    @SerializedName("problem_statement") val problemStatement: String,
    @SerializedName("market_size") val marketSize: String,
    @SerializedName("technical_implementation") val technicalImplementation: String,
    @SerializedName("architecture_decisions") val architectureDecisions: List<String>,
    @SerializedName("differentiation_strategy") val differentiationStrategy: String,
    @SerializedName("dataset_requirements") val datasetRequirements: String,
    @SerializedName("go_to_market") val goToMarket: String,
    @SerializedName("risk_assessment") val riskAssessment: String,
    @SerializedName("first_90_day_milestones") val first90DayMilestones: List<String>,
    @SerializedName("suggested_stack") val suggestedStack: List<String>
)

data class BlueprintsListResponse(
    val blueprints: List<BlueprintResponse>,
    val count: Int
)

data class PipelineResponse(
    val id: String,
    @SerializedName("technique_name") val techniqueName: String,
    @SerializedName("task_type") val taskType: String,
    @SerializedName("dataset_name") val datasetName: String,
    @SerializedName("model_architecture") val modelArchitecture: String,
    @SerializedName("notebook_content") val notebookContent: String,
    @SerializedName("colab_url") val colabUrl: String,
    val status: String,
    val metrics: Map<String, String>?,
    @SerializedName("model_card") val modelCard: String
)

data class PipelinesListResponse(
    val pipelines: List<PipelineResponse>,
    val count: Int
)

data class SearchResponse(
    val results: List<SearchResult>,
    val count: Int,
    val query: String
)

data class SearchResult(
    val id: String,
    val score: Double,
    val payload: Map<String, Any>?
)

data class VectorMapResponse(
    val points: List<VectorPoint>,
    val count: Int,
    @SerializedName("explained_variance") val explainedVariance: List<Double>?
)

data class VectorPoint(
    val x: Double,
    val y: Double,
    val title: String,
    val source: String,
    @SerializedName("novelty_score") val noveltyScore: Double,
    val categories: List<String>?
)

data class IngestResponse(
    val status: String,
    @SerializedName("signals_ingested") val signalsIngested: Int,
    @SerializedName("trends_updated") val trendsUpdated: Int
)

data class FeedbackResponse(
    val status: String,
    val feedback: Map<String, String>
)
