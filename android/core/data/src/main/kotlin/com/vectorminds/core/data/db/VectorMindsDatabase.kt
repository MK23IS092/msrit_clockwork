package com.vectorminds.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vectorminds.core.data.db.dao.TrendCacheDao
import com.vectorminds.core.data.db.dao.ActionLogDao
import com.vectorminds.core.data.db.entity.TrendCacheEntity
import com.vectorminds.core.data.db.entity.ActionLogEntity

/**
 * VectorMinds Room Database.
 * Caches trends, blueprints, and action logs locally for offline access.
 */
@Database(
    entities = [
        TrendCacheEntity::class,
        ActionLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VectorMindsDatabase : RoomDatabase() {
    abstract fun trendCacheDao(): TrendCacheDao
    abstract fun actionLogDao(): ActionLogDao
}
