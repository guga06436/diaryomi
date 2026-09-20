package com.diaryomi.domain.model

import java.time.LocalDate

/**
 * Modelo de domínio representando uma busca monitorada pelo usuário.
 * @property extensionId Identificador da extensão de diário (ex: "gov_federal")
 * @property extensionName Nome amigável da extensão para exibição na UI
 * @property terms Lista de termos obrigatórios cadastrados pelo usuário
 * @property startDate Data inicial definida pelo usuário
 * @property lastCheckedDate Última data verificada com sucesso pelo app
 * @property unreadCount Quantidade de resultados não lidos
 */
data class TrackedSearch(
    val id: Long = 0,
    val extensionId: String,
    val extensionName: String = "",
    val terms: List<String>,
    val startDate: LocalDate,
    val lastCheckedDate: LocalDate?,
    val unreadCount: Int = 0
)

