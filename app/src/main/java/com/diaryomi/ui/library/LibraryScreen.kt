package com.diaryomi.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.ui.library.components.AddSearchBottomSheet
import com.diaryomi.ui.library.components.EditTermsBottomSheet
import com.diaryomi.ui.library.components.SearchCard
import com.diaryomi.ui.library.components.SyncLogsDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddSearchSheet by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var searchToDelete by remember { mutableStateOf<TrackedSearch?>(null) }
    var searchToEditDate by remember { mutableStateOf<TrackedSearch?>(null) }
    var searchToEditTerms by remember { mutableStateOf<TrackedSearch?>(null) }

    // Mensagens de erro
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Mensagens informativas de sincronização
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSearchSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Nova busca") },
                text = { Text("Nova busca") }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = { viewModel.syncAll() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.searches.isEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ManageSearch,
                        contentDescription = "Sem buscas ativas",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sem buscas ativas",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adicione termos para monitorar publicações em Diários Oficiais",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showLogsDialog = true }) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Logs do Sistema")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    // Header customizado
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Diaryomi",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.searches.size} busca(s) monitorada(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }

                            // Botão para abrir o console de logs
                            IconButton(
                                onClick = { showLogsDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = "Ver Logs de Pesquisa")
                            }
                        }
                    }

                    items(uiState.searches, key = { it.id }) { search ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SearchCard(
                                search = search,
                                onClick = { onNavigateToDetail(search.id) },
                                onEditTermsClick = { searchToEditTerms = search },
                                onEditDateClick = { searchToEditDate = search },
                                onDeleteClick = { searchToDelete = search },
                                onMarkAllAsReadClick = { viewModel.markAllAsRead(search.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal de Cadastro
    if (showAddSearchSheet) {
        AddSearchBottomSheet(
            availableExtensions = uiState.availableExtensions,
            onDismiss = { showAddSearchSheet = false },
            onSave = { extensionId, terms, date ->
                viewModel.addSearch(extensionId, terms, date)
                showAddSearchSheet = false
            }
        )
    }

    // Modal de Edição de Termos
    searchToEditTerms?.let { search ->
        EditTermsBottomSheet(
            initialTerms = search.terms,
            onDismiss = { searchToEditTerms = null },
            onSave = { newTerms ->
                viewModel.updateTerms(search.id, newTerms)
                searchToEditTerms = null
            }
        )
    }

    // Modal de Logs
    if (showLogsDialog) {
        SyncLogsDialog(onDismiss = { showLogsDialog = false })
    }

    // Dialog de Confirmação de Exclusão
    searchToDelete?.let { search ->
        AlertDialog(
            onDismissRequest = { searchToDelete = null },
            title = { Text("Excluir busca monitorada?") },
            text = {
                Text(
                    "Deseja realmente remover os termos \"${search.terms.joinToString(", ")}\"? " +
                    "Todas as publicações salvas para esta busca também serão apagadas permanentemente."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSearch(search.id)
                        searchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { searchToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog de Edição de Data Inicial
    searchToEditDate?.let { search ->
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = search.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { searchToEditDate = null },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.editStartDate(search.id, newDate)
                    }
                    searchToEditDate = null
                }) {
                    Text("Salvar e Ressincronizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { searchToEditDate = null }) {
                    Text("Cancelar")
                }
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Atenção: alterar a data inicial apagará os resultados anteriores e reiniciará as consultas a partir da nova data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DatePicker(state = datePickerState)
            }
        }
    }
}
