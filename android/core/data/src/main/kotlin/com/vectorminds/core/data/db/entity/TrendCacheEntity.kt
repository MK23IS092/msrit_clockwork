package com.vectorminds.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trend_cache")
data class TrendCacheEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    @ColumnInfo(name = "technique_name") val techniqueName: String,
    val description: String,
    @ColumnInfo(name = "emergence_score") val emergenceScore: Double,
    @ColumnInfo(name = "novelty_score") val noveltyScore: Double,
    @ColumnInfo(name = "impact_score") val impactScore: Double,
    @ColumnInfo(name = "mainstream_eta_months") val mainstreamEtaMonths: Int,
    val confidence: Double,
    @ColumnInfo(name = "paper_count") val paperCount: Int,
    @ColumnInfo(name = "github_stars") val githubStars: Int,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
