package com.diaryomi.domain.model

import java.time.LocalDate

/**
 * Resultado bruto retornado por uma GazetteExtension antes de ser persistido.
 * @property matchedTerms Lista de termos que foram encontrados nesta publicação específica.
 */
data class GazetteResult(
    val title: String,
    val date: LocalDate,
    val snippet: String,
    val url: String,
    val matchedTerms: List<String> = emptyList()
)
