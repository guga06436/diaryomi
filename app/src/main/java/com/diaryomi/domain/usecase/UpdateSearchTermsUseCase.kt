package com.diaryomi.domain.usecase

import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.util.AppLogger
import javax.inject.Inject

/**
 * Caso de uso para editar e atualizar os termos de uma busca monitorada.
 * Limpa os resultados cacheados e reseta lastCheckedDate para permitir uma nova varredura completa.
 */
class UpdateSearchTermsUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(searchId: Long, newTerms: List<String>) {
        val cleanTerms = newTerms
            .map { it.trim().replace("\\s+".toRegex(), " ") }
            .filter { it.isNotBlank() }

        require(cleanTerms.isNotEmpty()) { "A lista de termos não pode ser vazia" }

        AppLogger.i("📝 Atualizando termos da busca #$searchId para: [${cleanTerms.joinToString(", ")}]")
        repository.updateSearchTerms(searchId, cleanTerms)
    }
}
