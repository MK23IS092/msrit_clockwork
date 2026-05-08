package com.vectorminds.core.data.db.dao

import androidx.room.*
import com.vectorminds.core.data.db.entity.TrendCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrendCacheDao {
    @Query("SELECT * FROM trend_cache ORDER BY rank ASC")
    fun getAll(): Flow<List<TrendCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trends: List<TrendCacheEntity>)

    @Query("DELETE FROM trend_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM trend_cache")
    suspend fun count(): Int
}
