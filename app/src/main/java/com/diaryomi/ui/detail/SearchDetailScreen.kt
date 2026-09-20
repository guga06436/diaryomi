package com.diaryomi.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diaryomi.domain.model.SearchResult
import com.diaryomi.ui.library.components.EditTermsBottomSheet
import com.diaryomi.ui.library.components.SyncLogsDialog
import com.diaryomi.util.TextNormalizer
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showEditTermsSheet by remember { mutableStateOf(false) }
    var topMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedResultIds.size} selecionada(s)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar seleção")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Selecionar todas")
                        }
                        IconButton(
                            onClick = { viewModel.markSelectedAsRead() },
                            enabled = uiState.selectedResultIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Marcar selecionadas como lidas")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.search?.terms?.joinToString(" • ") ?: "Detalhes da Busca",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            uiState.search?.extensionName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        val hasUnread = uiState.results.any { !it.isRead }
                        if (hasUnread) {
                            IconButton(onClick = { viewModel.markAllAsRead() }) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Marcar todas como lidas")
                            }
                        }
                        if (uiState.results.isNotEmpty()) {
                            IconButton(onClick = { viewModel.enterSelectionMode() }) {
                                Icon(Icons.Default.Checklist, contentDescription = "Selecionar publicações")
                            }
                        }
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar termos de busca") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    topMenuExpanded = false
                                    showEditTermsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ver logs da consulta") },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                                onClick = {
                                    topMenuExpanded = false
                                    showLogsDialog = true
                                }
                            )
                            if (hasUnread) {
                                DropdownMenuItem(
                                    text = { Text("Marcar todas como lidas") },
                                    leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                                    onClick = {
                                        topMenuExpanded = false
                                        viewModel.markAllAsRead()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = { viewModel.syncSearch() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.results.isEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = "Nenhum resultado",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum resultado",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Puxe para baixo para buscar ou confira os logs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showLogsDialog = true }) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Logs da Consulta")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val groupedResults = uiState.results.groupBy { it.date }
                    val searchTerms = uiState.search?.terms ?: emptyList()

                    groupedResults.forEach { (date, resultsForDate) ->
                        stickyHeader(key = "header_${date}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(top = 16.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }
                        }
                        
                        items(resultsForDate, key = { it.id }) { result ->
                            val isSelected = uiState.selectedResultIds.contains(result.id)
                            ResultCard(
                                result = result,
                                searchTerms = searchTerms,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = { viewModel.toggleSelection(result.id) },
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.enterSelectionMode(result.id)
                                    }
                                },
                                onMarkAsReadClick = {
                                    viewModel.markAsRead(result.id)
                                },
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(result.id)
                                    } else {
                                        viewModel.openResult(context, result)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditTermsSheet) {
        uiState.search?.let { search ->
            EditTermsBottomSheet(
                initialTerms = search.terms,
                onDismiss = { showEditTermsSheet = false },
                onSave = { newTerms ->
                    viewModel.updateTerms(newTerms)
                    showEditTermsSheet = false
                }
            )
        }
    }

    if (showLogsDialog) {
        SyncLogsDialog(onDismiss = { showLogsDialog = false })
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ResultCard(
    result: SearchResult,
    searchTerms: List<String>,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onMarkAsReadClick: () -> Unit,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        !result.isRead -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isSelected || !result.isRead) 2.dp else 1.dp
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    // Identifica quais termos foram encontrados nesta matéria
    val termsFound = if (result.matchedTerms.isNotEmpty()) {
        result.matchedTerms
    } else {
        searchTerms.filter { term ->
            TextNormalizer.containsIgnoreCaseAndAccents("${result.title} ${result.snippet}", term)
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Se estiver em modo de seleção, exibe checkbox no topo
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                    if (!result.isRead) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Não lida",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Título
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (!result.isRead) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Badges dos termos encontrados no achado
            if (termsFound.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    termsFound.forEach { term ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Snippet
            Text(
                text = result.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rodapé
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = result.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!result.isRead && !isSelectionMode) {
                        OutlinedButton(
                            onClick = onMarkAsReadClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Marcar lida",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!isSelectionMode) {
                        IconButton(
                            onClick = onClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir no navegador",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
