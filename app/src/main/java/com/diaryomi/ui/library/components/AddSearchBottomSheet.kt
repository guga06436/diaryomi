package com.diaryomi.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.diaryomi.extension.GazetteExtension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSearchBottomSheet(
    availableExtensions: List<GazetteExtension>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, LocalDate) -> Unit
) {
    var selectedExtension by remember { mutableStateOf<GazetteExtension?>(availableExtensions.firstOrNull()) }
    var currentTerm by remember { mutableStateOf("") }
    val terms = remember { mutableStateListOf<String>() }
    var initialDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nova Busca",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Section 1: Diário Oficial
            Text(
                text = "Diário Oficial",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedExtension?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableExtensions.forEach { extension ->
                        DropdownMenuItem(
                            text = { Text(extension.name) },
                            onClick = {
                                selectedExtension = extension
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Termos de busca
            Text(
                text = "Termos de busca",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Adicione os termos que deseja monitorar (pressione Enter para adicionar)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            fun addTerm() {
                val clean = currentTerm.trim().replace("\\s+".toRegex(), " ")
                if (clean.isNotBlank()) {
                    val alreadyAdded = terms.any {
                        com.diaryomi.util.TextNormalizer.normalize(it) == com.diaryomi.util.TextNormalizer.normalize(clean)
                    }
                    if (!alreadyAdded) {
                        terms.add(clean)
                    }
                    currentTerm = ""
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = currentTerm,
                    onValueChange = { currentTerm = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ex: Previdência") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTerm() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { addTerm() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar termo")
                }
            }

            if (terms.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    terms.forEach { term ->
                        InputChip(
                            selected = false,
                            onClick = { terms.remove(term) },
                            label = { Text(term) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover termo",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Data inicial
            Text(
                text = "Data inicial",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Buscar publicações a partir de",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Box {
                OutlinedTextField(
                    value = initialDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar data")
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                initialDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val isValid = selectedExtension != null && terms.isNotEmpty()
            FilledTonalButton(
                onClick = {
                    selectedExtension?.let {
                        onSave(it.id, terms.toList(), initialDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar busca")
            }
        }
    }
}
