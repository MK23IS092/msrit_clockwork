package com.vectorminds.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_log")
data class ActionLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "skill_id") val skillId: String,
    val description: String,
    val status: String, // "success", "failed", "skipped"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
