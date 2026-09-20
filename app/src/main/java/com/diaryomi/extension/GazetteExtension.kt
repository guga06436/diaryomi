package com.diaryomi.extension

import com.diaryomi.domain.model.GazetteResult
import java.time.LocalDate

/**
 * Interface base para extensões de Diários Oficiais (Strategy Pattern).
 * Cada diário oficial implementa esta interface para definir como buscar publicações.
 */
interface GazetteExtension {
    /** Identificador único da extensão (ex: "gov_federal", "tce_rn") */
    val id: String
    /** Nome amigável da extensão para exibição na UI */
    val name: String
    /**
     * Executa busca no diário oficial.
     * @param terms Lista de termos obrigatórios a serem buscados
     * @param fromDate Data inicial (inclusive)
     * @param toDate Data final (inclusive)
     * @return Lista de resultados encontrados
     */
    suspend fun search(terms: List<String>, fromDate: LocalDate, toDate: LocalDate): List<GazetteResult>

    /**
     * Resolve a URL final para visualização quando o usuário clica no resultado.
     * Por padrão retorna a própria URL armazenada, mas extensões como TCE-RN podem
     * gerar uma nova URL temporária a cada clique.
     */
    suspend fun resolveUrl(url: String): String = url
}
