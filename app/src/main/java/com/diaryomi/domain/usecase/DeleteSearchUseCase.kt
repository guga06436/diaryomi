package com.diaryomi.domain.usecase

import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.util.AppLogger
import javax.inject.Inject

/**
 * Caso de uso para excluir uma busca monitorada e todos os seus resultados associados (CASCADE).
 */
class DeleteSearchUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(searchId: Long) {
        AppLogger.i("🗑️ Excluindo busca #$searchId e seus resultados vinculados.")
        repository.deleteSearch(searchId)
    }
}
