package com.diaryomi.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.domain.usecase.*
import com.diaryomi.extension.GazetteExtension
import com.diaryomi.extension.GazetteExtensionRegistry
import com.diaryomi.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LibraryUiState(
    val searches: List<TrackedSearch> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val syncMessage: String? = null,
    val availableExtensions: List<GazetteExtension> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getSearchesUseCase: GetSearchesWithResultsUseCase,
    private val addSearchUseCase: AddSearchUseCase,
    private val deleteSearchUseCase: DeleteSearchUseCase,
    private val editSearchDateUseCase: EditSearchDateUseCase,
    private val updateSearchTermsUseCase: UpdateSearchTermsUseCase,
    private val syncUseCase: SyncUseCase,
    private val markResultReadUseCase: MarkResultReadUseCase,
    private val extensionRegistry: GazetteExtensionRegistry
) : ViewModel() {

    fun markAllAsRead(searchId: Long) {
        viewModelScope.launch {
            try {
                markResultReadUseCase.markAll(searchId)
            } catch (e: Exception) {
                AppLogger.e("Erro ao marcar todas como lidas para a busca #$searchId", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(availableExtensions = extensionRegistry.getAll()) }
        
        viewModelScope.launch {
            getSearchesUseCase()
                .catch { e ->
                    AppLogger.e("Erro ao carregar buscas", e)
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { searches ->
                    _uiState.update { it.copy(searches = searches, isLoading = false) }
                }
        }
    }

    fun addSearch(extensionId: String, terms: List<String>, startDate: LocalDate) {
        viewModelScope.launch {
            try {
                AppLogger.i("Cadastrando nova busca: extensão '$extensionId', termos: ${terms.joinToString()}, desde $startDate")
                val searchId = addSearchUseCase(extensionId, terms, startDate)
                _uiState.update { it.copy(syncMessage = "Busca cadastrada com sucesso! Iniciando primeira consulta...") }
                // Imediatamente dispara a primeira busca
                syncUseCase.syncSearch(searchId)
            } catch (e: Exception) {
                AppLogger.e("Erro ao cadastrar busca", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSearch(searchId: Long) {
        viewModelScope.launch {
            try {
                deleteSearchUseCase(searchId)
                _uiState.update { it.copy(syncMessage = "Busca removida com sucesso.") }
            } catch (e: Exception) {
                AppLogger.e("Erro ao excluir busca", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun editStartDate(searchId: Long, newStartDate: LocalDate) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSyncing = true) }
                editSearchDateUseCase(searchId, newStartDate)
                _uiState.update { it.copy(syncMessage = "Data atualizada. Ressincronizando...") }
                val newCount = syncUseCase.syncSearch(searchId)
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        syncMessage = "Ressincronização concluída: $newCount nova(s) publicação(ões) encontrada(s)."
                    ) 
                }
            } catch (e: Exception) {
                AppLogger.e("Erro ao atualizar data", e)
                _uiState.update { it.copy(error = e.message, isSyncing = false) }
            }
        }
    }

    fun updateTerms(searchId: Long, newTerms: List<String>) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSyncing = true) }
                updateSearchTermsUseCase(searchId, newTerms)
                _uiState.update { it.copy(syncMessage = "Termos atualizados! Ressincronizando busca...") }
                val newCount = syncUseCase.syncSearch(searchId)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = "Ressincronização concluída: $newCount nova(s) publicação(ões) encontrada(s)."
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("Erro ao atualizar termos da busca #$searchId", e)
                _uiState.update { it.copy(error = e.message, isSyncing = false) }
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                AppLogger.i("Disparada sincronização manual via Pull-to-Refresh.")
                val totalNew = syncUseCase.syncAll()
                val message = if (totalNew > 0) {
                    "Sincronização concluída: $totalNew nova(s) publicação(ões) encontrada(s)!"
                } else {
                    "Sincronização concluída. Nenhuma nova publicação encontrada."
                }
                _uiState.update { it.copy(syncMessage = message) }
            } catch (e: Exception) {
                AppLogger.e("Erro ao sincronizar diários", e)
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
}
