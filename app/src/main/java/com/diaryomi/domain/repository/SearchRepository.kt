package com.diaryomi.domain.repository

import com.diaryomi.domain.model.GazetteResult
import com.diaryomi.domain.model.SearchResult
import com.diaryomi.domain.model.TrackedSearch
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SearchRepository {
    fun observeAllSearches(): Flow<List<TrackedSearch>>
    fun observeResultsForSearch(searchId: Long): Flow<List<SearchResult>>
    suspend fun addSearch(search: TrackedSearch): Long
    suspend fun getSearchById(searchId: Long): TrackedSearch?
    suspend fun getAllSearches(): List<TrackedSearch>
    suspend fun updateLastCheckedDate(searchId: Long, date: LocalDate)
    suspend fun saveResults(searchId: Long, results: List<GazetteResult>): Int
    suspend fun markResultRead(resultId: Long)
    suspend fun markResultsRead(resultIds: List<Long>)
    suspend fun markAllResultsRead(searchId: Long)
    suspend fun editStartDate(searchId: Long, newStartDate: LocalDate)
    suspend fun updateSearchTerms(searchId: Long, newTerms: List<String>)
    suspend fun deleteSearch(searchId: Long)
}
