package com.diaryomi.domain.usecase

import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.util.AppLogger
import java.time.LocalDate
import javax.inject.Inject

/**
 * Caso de uso para alterar a data inicial de uma busca.
 * Conforme regra de negócio: reseta lastCheckedDate e limpa os resultados antigos.
 */
class EditSearchDateUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(searchId: Long, newStartDate: LocalDate) {
        AppLogger.i("📅 Alterando data inicial da busca #$searchId para $newStartDate (resetando histórico e resultados anteriores).")
        repository.editStartDate(searchId, newStartDate)
    }
}
