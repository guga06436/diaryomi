package com.diaryomi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diaryomi.data.local.entity.SearchResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<SearchResultEntity>)

    @Query("SELECT * FROM search_results WHERE searchId = :searchId ORDER BY date DESC")
    fun observeBySearchId(searchId: Long): Flow<List<SearchResultEntity>>

    @Query("SELECT COUNT(*) FROM search_results WHERE searchId = :searchId AND isRead = 0")
    fun countUnread(searchId: Long): Flow<Int>

    @Query("SELECT * FROM search_results WHERE searchId = :searchId")
    suspend fun getResultsBySearchId(searchId: Long): List<SearchResultEntity>

    @Query("UPDATE search_results SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE search_results SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markRead(ids: List<Long>)

    @Query("UPDATE search_results SET isRead = 1 WHERE searchId = :searchId")
    suspend fun markAllReadBySearchId(searchId: Long)

    @Query("DELETE FROM search_results WHERE searchId = :searchId")
    suspend fun deleteBySearchId(searchId: Long)
}
