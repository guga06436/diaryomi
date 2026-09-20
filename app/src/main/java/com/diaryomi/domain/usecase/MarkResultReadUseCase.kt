package com.diaryomi.domain.usecase

import com.diaryomi.domain.repository.SearchRepository
import javax.inject.Inject

class MarkResultReadUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(resultId: Long) {
        repository.markResultRead(resultId)
    }

    suspend fun markMultiple(resultIds: List<Long>) {
        repository.markResultsRead(resultIds)
    }

    suspend fun markAll(searchId: Long) {
        repository.markAllResultsRead(searchId)
    }
}
