package com.vectorminds.core.network

import retrofit2.http.*

/**
 * Retrofit interface for communicating with the VectorMinds Python backend.
 * All endpoints match the FastAPI routes defined in backend/delivery/api_routes.py.
 */
interface VectorMindsApi {

    @GET("api/health")
    suspend fun healthCheck(): HealthResponse

    @GET("api/stats")
    suspend fun getStats(): StatsResponse

    @GET("api/dashboard/premium-context")
    suspend fun getDashboardPremiumContext(): DashboardPremiumContextResponse

    @POST("api/ingest")
    suspend fun triggerIngestion(@Body request: IngestRequest): IngestResponse

    @GET("api/ingest/status")
    suspend fun getIngestionStatus(): IngestionStatus

    @GET("api/trends")
    suspend fun getTrends(@Query("limit") limit: Int = 20): TrendsResponse

    @GET("api/trends/{trendId}")
    suspend fun getTrendDetail(
        @Path("trendId") trendId: String,
        @Query("include_brief") includeBrief: Boolean = false,
    ): TrendDetail

    @POST("api/blueprints/generate")
    suspend fun generateBlueprint(@Body request: BlueprintRequest): BlueprintResponse

    @GET("api/blueprints")
    suspend fun listBlueprints(): BlueprintsListResponse

    @GET("api/blueprints/{blueprintId}")
    suspend fun getBlueprint(@Path("blueprintId") blueprintId: String): BlueprintResponse

    @POST("api/pipelines/generate")
    suspend fun generatePipeline(@Body request: PipelineRequest): PipelineResponse

    @POST("api/pipelines/dataset-candidates")
    suspend fun getDatasetCandidates(@Body request: DatasetCandidatesRequest): DatasetCandidatesResponse

    @GET("api/pipelines")
    suspend fun listPipelines(): PipelinesListResponse

    @POST("api/search")
    suspend fun semanticSearch(@Body request: SearchRequest): SearchResponse

    @GET("api/vector-map")
    suspend fun getVectorMap(@Query("limit") limit: Int = 200): VectorMapResponse

    @POST("api/feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): FeedbackResponse
}
