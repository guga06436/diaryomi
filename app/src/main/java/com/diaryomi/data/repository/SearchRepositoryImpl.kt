package com.diaryomi.data.repository

import com.diaryomi.data.local.SearchResultDao
import com.diaryomi.data.local.TrackedSearchDao
import com.diaryomi.data.local.entity.SearchResultEntity
import com.diaryomi.data.local.entity.TrackedSearchEntity
import com.diaryomi.domain.model.GazetteResult
import com.diaryomi.domain.model.SearchResult
import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.extension.GazetteExtensionRegistry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementação concreta do repositório de buscas.
 * Combina DAOs do Room com o registro de extensões para construir modelos de domínio completos.
 */
class SearchRepositoryImpl @Inject constructor(
    private val trackedSearchDao: TrackedSearchDao,
    private val searchResultDao: SearchResultDao,
    private val gson: Gson,
    private val extensionRegistry: GazetteExtensionRegistry
) : SearchRepository {

    private val termsType = object : TypeToken<List<String>>() {}.type

    /** Resolve o nome amigável da extensão pelo ID */
    private fun resolveExtensionName(extensionId: String): String {
        return extensionRegistry.getById(extensionId)?.name ?: extensionId
    }

    override fun observeAllSearches(): Flow<List<TrackedSearch>> {
        return trackedSearchDao.observeAllWithUnreadCount().map { list ->
            list.map { item ->
                TrackedSearch(
                    id = item.id,
                    extensionId = item.extensionId,
                    extensionName = resolveExtensionName(item.extensionId),
                    terms = gson.fromJson(item.terms, termsType),
                    startDate = LocalDate.ofEpochDay(item.startDate),
                    lastCheckedDate = item.lastCheckedDate?.let { LocalDate.ofEpochDay(it) },
                    unreadCount = item.unreadCount
                )
            }
        }
    }

    override fun observeResultsForSearch(searchId: Long): Flow<List<SearchResult>> {
        return searchResultDao.observeBySearchId(searchId).map { entities ->
            entities.map { entity ->
                SearchResult(
                    id = entity.id,
                    searchId = entity.searchId,
                    title = entity.title,
                    date = LocalDate.ofEpochDay(entity.date),
                    snippet = entity.snippet,
                    url = entity.url,
                    isRead = entity.isRead,
                    matchedTerms = try {
                        if (entity.matchedTerms.isNotBlank()) {
                            gson.fromJson(entity.matchedTerms, termsType)
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                )
            }
        }
    }

    override suspend fun addSearch(search: TrackedSearch): Long {
        val entity = TrackedSearchEntity(
            extensionId = search.extensionId,
            terms = gson.toJson(search.terms),
            startDate = search.startDate.toEpochDay(),
            lastCheckedDate = search.lastCheckedDate?.toEpochDay()
        )
        return trackedSearchDao.insert(entity)
    }

    override suspend fun getSearchById(searchId: Long): TrackedSearch? {
        val entity = trackedSearchDao.getById(searchId) ?: return null
        return TrackedSearch(
            id = entity.id,
            extensionId = entity.extensionId,
            extensionName = resolveExtensionName(entity.extensionId),
            terms = gson.fromJson(entity.terms, termsType),
            startDate = LocalDate.ofEpochDay(entity.startDate),
            lastCheckedDate = entity.lastCheckedDate?.let { LocalDate.ofEpochDay(it) },
            unreadCount = 0
        )
    }

    override suspend fun getAllSearches(): List<TrackedSearch> {
        return trackedSearchDao.getAll().map { entity ->
            TrackedSearch(
                id = entity.id,
                extensionId = entity.extensionId,
                extensionName = resolveExtensionName(entity.extensionId),
                terms = gson.fromJson(entity.terms, termsType),
                startDate = LocalDate.ofEpochDay(entity.startDate),
                lastCheckedDate = entity.lastCheckedDate?.let { LocalDate.ofEpochDay(it) },
                unreadCount = 0
            )
        }
    }

    override suspend fun updateLastCheckedDate(searchId: Long, date: LocalDate) {
        trackedSearchDao.updateLastCheckedDate(searchId, date.toEpochDay())
    }

    override suspend fun saveResults(searchId: Long, results: List<GazetteResult>): Int {
        if (results.isEmpty()) return 0

        val existingEntities = searchResultDao.getResultsBySearchId(searchId)
        val seenKeys = existingEntities.map { existing ->
            "${existing.url.ifBlank { existing.title }}_${existing.date}"
        }.toMutableSet()

        val newEntities = mutableListOf<SearchResultEntity>()

        for (result in results) {
            val resultEpochDay = result.date.toEpochDay()
            val alreadyExists = existingEntities.any { existing ->
                (existing.url.isNotBlank() && result.url.isNotBlank() &&
                    (existing.url == result.url || 
                     (existing.url.substringBefore("#") == result.url.substringBefore("#") && existing.title == result.title))) ||
                (existing.title == result.title && existing.date == resultEpochDay)
            }

            val key = "${result.url.ifBlank { result.title }}_$resultEpochDay"
            if (!alreadyExists && seenKeys.add(key)) {
                newEntities.add(
                    SearchResultEntity(
                        searchId = searchId,
                        title = result.title,
                        date = resultEpochDay,
                        snippet = result.snippet,
                        url = result.url,
                        isRead = false,
                        matchedTerms = gson.toJson(result.matchedTerms)
                    )
                )
            }
        }

        if (newEntities.isNotEmpty()) {
            searchResultDao.insertAll(newEntities)
        }
        return newEntities.size
    }

    override suspend fun markResultRead(resultId: Long) {
        searchResultDao.markRead(resultId)
    }

    override suspend fun markResultsRead(resultIds: List<Long>) {
        if (resultIds.isNotEmpty()) {
            searchResultDao.markRead(resultIds)
        }
    }

    override suspend fun markAllResultsRead(searchId: Long) {
        searchResultDao.markAllReadBySearchId(searchId)
    }

    override suspend fun editStartDate(searchId: Long, newStartDate: LocalDate) {
        trackedSearchDao.updateStartDate(searchId, newStartDate.toEpochDay())
        searchResultDao.deleteBySearchId(searchId)
    }

    override suspend fun updateSearchTerms(searchId: Long, newTerms: List<String>) {
        trackedSearchDao.updateTerms(searchId, gson.toJson(newTerms))
        searchResultDao.deleteBySearchId(searchId)
    }

    override suspend fun deleteSearch(searchId: Long) {
        trackedSearchDao.delete(searchId)
    }
}
