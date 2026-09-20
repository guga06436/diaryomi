package com.diaryomi.domain.usecase

import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.domain.repository.SearchRepository
import java.time.LocalDate
import javax.inject.Inject

class AddSearchUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(extensionId: String, terms: List<String>, startDate: LocalDate): Long {
        val cleanTerms = terms
            .map { it.trim().replace("\\s+".toRegex(), " ") }
            .filter { it.isNotBlank() }

        require(cleanTerms.isNotEmpty()) { "A lista de termos não pode ser vazia" }
        require(extensionId.isNotBlank()) { "O identificador da extensão não pode ser vazio" }

        val search = TrackedSearch(
            extensionId = extensionId.trim(),
            terms = cleanTerms,
            startDate = startDate,
            lastCheckedDate = null
        )
        return repository.addSearch(search)
    }
}
