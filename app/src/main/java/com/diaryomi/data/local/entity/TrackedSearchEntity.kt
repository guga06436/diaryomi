package com.diaryomi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_searches")
data class TrackedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val extensionId: String,
    val terms: String,          // JSON array string
    val startDate: Long,        // epoch day
    val lastCheckedDate: Long?  // epoch day, nullable
)
