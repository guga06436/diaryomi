package com.diaryomi.domain.model

import java.time.LocalDate

/**
 * Resultado de busca persistido, vinculado a uma TrackedSearch.
 * @property matchedTerms Lista dos termos pesquisados que foram identificados nesta matéria.
 */
data class SearchResult(
    val id: Long = 0,
    val searchId: Long,
    val title: String,
    val date: LocalDate,
    val snippet: String,
    val url: String,
    val isRead: Boolean = false,
    val matchedTerms: List<String> = emptyList()
)
