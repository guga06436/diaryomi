package com.diaryomi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_results",
    foreignKeys = [ForeignKey(
        entity = TrackedSearchEntity::class,
        parentColumns = ["id"],
        childColumns = ["searchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["searchId"])]
)
data class SearchResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val searchId: Long,
    val title: String,
    val date: Long,             // epoch day
    val snippet: String,
    val url: String,
    val isRead: Boolean = false,
    val matchedTerms: String = "" // JSON list de termos encontrados
)
