package com.diaryomi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.diaryomi.data.local.entity.TrackedSearchEntity
import kotlinx.coroutines.flow.Flow

data class TrackedSearchWithUnreadCount(
    val id: Long,
    val extensionId: String,
    val terms: String,
    val startDate: Long,
    val lastCheckedDate: Long?,
    val unreadCount: Int
)

@Dao
interface TrackedSearchDao {
    @Insert
    suspend fun insert(entity: TrackedSearchEntity): Long

    @Query("SELECT * FROM tracked_searches")
    fun observeAll(): Flow<List<TrackedSearchEntity>>

    @Query("SELECT * FROM tracked_searches")
    suspend fun getAll(): List<TrackedSearchEntity>

    @Query("SELECT * FROM tracked_searches WHERE id = :id")
    suspend fun getById(id: Long): TrackedSearchEntity?

    @Query("UPDATE tracked_searches SET lastCheckedDate = :date WHERE id = :id")
    suspend fun updateLastCheckedDate(id: Long, date: Long)

    @Query("UPDATE tracked_searches SET startDate = :startDate, lastCheckedDate = NULL WHERE id = :id")
    suspend fun updateStartDate(id: Long, startDate: Long)

    @Query("UPDATE tracked_searches SET terms = :terms, lastCheckedDate = NULL WHERE id = :id")
    suspend fun updateTerms(id: Long, terms: String)

    @Query("DELETE FROM tracked_searches WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("""
        SELECT ts.*, COUNT(CASE WHEN sr.isRead = 0 THEN 1 END) as unreadCount
        FROM tracked_searches ts
        LEFT JOIN search_results sr ON ts.id = sr.searchId
        GROUP BY ts.id
    """)
    fun observeAllWithUnreadCount(): Flow<List<TrackedSearchWithUnreadCount>>
}
