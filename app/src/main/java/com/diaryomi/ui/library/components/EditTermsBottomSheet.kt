package com.diaryomi.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.diaryomi.util.TextNormalizer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTermsBottomSheet(
    initialTerms: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val terms = remember { mutableStateListOf<String>().apply { addAll(initialTerms) } }
    var currentTerm by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    fun addTerm() {
        val clean = currentTerm.trim().replace("\\s+".toRegex(), " ")
        if (clean.isNotBlank()) {
            val alreadyAdded = terms.any {
                TextNormalizer.normalize(it) == TextNormalizer.normalize(clean)
            }
            if (!alreadyAdded) {
                terms.add(clean)
            }
            currentTerm = ""
        }
    }

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
                Column {
                    Text(
                        text = "Editar Termos de Busca",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Adicione ou remova termos monitorados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Campo para adicionar novo termo
            Text(
                text = "Adicionar novo termo",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = currentTerm,
                    onValueChange = { currentTerm = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Digite o termo ou nome...") },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de termos atuais como Chips
            Text(
                text = "Termos atuais (${terms.size})",
                style = MaterialTheme.typography.labelLarge
            )

            if (terms.isEmpty()) {
                Text(
                    text = "Nenhum termo cadastrado. Adicione ao menos um termo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
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
                                    contentDescription = "Remover $term",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão Salvar
            Button(
                onClick = {
                    if (terms.isNotEmpty()) {
                        onSave(terms.toList())
                    }
                },
                enabled = terms.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Termos e Ressincronizar")
            }
        }
    }
}
