package com.diaryomi.domain.usecase

import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchesWithResultsUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    operator fun invoke(): Flow<List<TrackedSearch>> {
        return repository.observeAllSearches()
    }
}
