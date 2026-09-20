package com.diaryomi.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diaryomi.domain.model.SearchResult
import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.domain.usecase.MarkResultReadUseCase
import com.diaryomi.domain.usecase.SyncUseCase
import com.diaryomi.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.diaryomi.extension.GazetteExtensionRegistry
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

data class SearchDetailUiState(
    val search: TrackedSearch? = null,
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val selectedResultIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository,
    private val markResultReadUseCase: MarkResultReadUseCase,
    private val syncUseCase: SyncUseCase,
    private val updateSearchTermsUseCase: com.diaryomi.domain.usecase.UpdateSearchTermsUseCase,
    private val extensionRegistry: GazetteExtensionRegistry,
    private val httpClient: OkHttpClient
) : ViewModel() {
    private val searchId: Long = savedStateHandle.get<Long>("searchId") ?: 0L

    private val _uiState = MutableStateFlow(SearchDetailUiState(isLoading = true))
    val uiState: StateFlow<SearchDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val search = searchRepository.getSearchById(searchId)
                _uiState.update { it.copy(search = search, isLoading = false) }
            } catch (e: Exception) {
                AppLogger.e("Erro ao carregar detalhes da busca #$searchId", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }

            searchRepository.observeResultsForSearch(searchId)
                .catch { e ->
                    AppLogger.e("Erro ao observar resultados da busca #$searchId", e)
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { results ->
                    _uiState.update { it.copy(results = results) }
                }
        }
    }

    fun syncSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                AppLogger.i("Disparada sincronização manual para a busca #$searchId.")
                val newCount = syncUseCase.syncSearch(searchId)
                val search = searchRepository.getSearchById(searchId)
                _uiState.update { it.copy(search = search) }
                AppLogger.i("Sincronização da busca #$searchId concluída: $newCount nova(s) publicação(ões).")
            } catch (e: Exception) {
                AppLogger.e("Erro ao sincronizar busca #$searchId", e)
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun markAsRead(resultId: Long) {
        viewModelScope.launch {
            try {
                markResultReadUseCase(resultId)
            } catch (e: Exception) {
                AppLogger.e("Erro ao marcar como lido", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleSelection(resultId: Long) {
        _uiState.update { state ->
            val newSelected = if (state.selectedResultIds.contains(resultId)) {
                state.selectedResultIds - resultId
            } else {
                state.selectedResultIds + resultId
            }
            state.copy(
                selectedResultIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                selectedResultIds = state.results.map { it.id }.toSet(),
                isSelectionMode = true
            )
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            state.copy(
                selectedResultIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun enterSelectionMode(initialId: Long? = null) {
        _uiState.update { state ->
            val set = if (initialId != null) setOf(initialId) else emptySet()
            state.copy(
                isSelectionMode = true,
                selectedResultIds = set
            )
        }
    }

    fun markSelectedAsRead() {
        val selectedIds = _uiState.value.selectedResultIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            try {
                markResultReadUseCase.markMultiple(selectedIds)
                clearSelection()
            } catch (e: Exception) {
                AppLogger.e("Erro ao marcar selecionados como lidos", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                markResultReadUseCase.markAll(searchId)
                clearSelection()
            } catch (e: Exception) {
                AppLogger.e("Erro ao marcar todos como lidos", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTerms(newTerms: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                updateSearchTermsUseCase(searchId, newTerms)
                val search = searchRepository.getSearchById(searchId)
                _uiState.update { it.copy(search = search) }
                syncUseCase.syncSearch(searchId)
            } catch (e: Exception) {
                AppLogger.e("Erro ao atualizar termos da busca #$searchId", e)
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    /**
     * Marca o resultado como lido e abre a publicação/PDF.
     * No módulo TCE-RN (ou qualquer extensão que implemente resolveUrl), gera uma nova URL
     * direta para o PDF a cada clique antes de disparar o visualizador/navegador.
     */
    fun openResult(context: Context, result: SearchResult) {
        markAsRead(result.id)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extension = _uiState.value.search?.extensionId?.let { extensionRegistry.getById(it) }
                val resolvedUrl = extension?.resolveUrl(result.url) ?: result.url

                withContext(Dispatchers.Main) {
                    launchIntent(context, resolvedUrl)
                }
            } catch (e: Exception) {
                AppLogger.e("Erro ao resolver URL para o resultado: ${result.url}", e)
                withContext(Dispatchers.Main) {
                    launchIntent(context, result.url)
                }
            }
        }
    }

    private fun launchIntent(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e("Não foi possível abrir o link: $url", e)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
