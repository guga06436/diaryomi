package com.diaryomi.domain.usecase

import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.extension.GazetteExtensionRegistry
import com.diaryomi.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * Caso de uso responsável pela sincronização de buscas monitoradas.
 * Retorna o número total de novos resultados encontrados com logging em tempo real.
 */
class SyncUseCase @Inject constructor(
    private val repository: SearchRepository,
    private val extensionRegistry: GazetteExtensionRegistry
) {
    /**
     * Sincroniza todas as buscas monitoradas.
     * @return Número total de novos resultados encontrados
     */
    suspend fun syncAll(): Int = withContext(Dispatchers.IO) {
        val searches = repository.getAllSearches()
        if (searches.isEmpty()) {
            AppLogger.i("Nenhuma busca monitorada para sincronizar.")
            return@withContext 0
        }

        AppLogger.i("Iniciando sincronização de ${searches.size} busca(s)...")
        var totalNewResults = 0

        for (search in searches) {
            try {
                totalNewResults += syncSearch(search.id)
            } catch (e: Exception) {
                AppLogger.e("Falha ao sincronizar busca #${search.id} (${search.terms.joinToString()}): ${e.message}", e)
            }
        }

        AppLogger.i("Sincronização geral finalizada. Total de novos resultados: $totalNewResults")
        totalNewResults
    }

    /**
     * Sincroniza uma busca específica.
     * @return Número de novos resultados encontrados
     */
    suspend fun syncSearch(searchId: Long): Int = withContext(Dispatchers.IO) {
        val search = repository.getSearchById(searchId) ?: run {
            AppLogger.w("Busca ID #$searchId não encontrada no banco de dados.")
            return@withContext 0
        }

        val extension = extensionRegistry.getById(search.extensionId)
        if (extension == null) {
            AppLogger.w("Extensão '${search.extensionId}' não encontrada no registro de diários.")
            return@withContext 0
        }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val fromDate = when {
            search.lastCheckedDate != null && search.lastCheckedDate.isBefore(today) -> search.lastCheckedDate
            search.lastCheckedDate == null && search.startDate.isBefore(today) -> search.startDate
            else -> today
        }
        val toDate = tomorrow

        AppLogger.i("Sincronizando busca #${search.id} [${extension.name}] para termos: [${search.terms.joinToString(", ")}] | Período: $fromDate a $toDate")

        val results = extension.search(search.terms, fromDate, toDate)

        val newResultsCount = if (results.isNotEmpty()) {
            val count = repository.saveResults(searchId, results)
            if (count > 0) {
                AppLogger.i("💾 Salvo(s) $count novo(s) resultado(s) inédito(s) para a busca #${search.id} (de ${results.size} matéria(s) encontrada(s)).")
            } else {
                AppLogger.d("Todas as ${results.size} matérias encontradas para a busca #${search.id} já estavam salvas anteriormente.")
            }
            count
        } else {
            AppLogger.d("Nenhuma matéria encontrada para a busca #${search.id}.")
            0
        }

        repository.updateLastCheckedDate(searchId, today)
        newResultsCount
    }
}
