package com.vectorminds.core.data.db.dao

import androidx.room.*
import com.vectorminds.core.data.db.entity.ActionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_log ORDER BY created_at DESC")
    fun getAll(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActionLogEntity)

    @Query("SELECT COUNT(*) FROM action_log")
    suspend fun count(): Int
}
