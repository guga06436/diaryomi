package com.diaryomi.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.diaryomi.domain.model.TrackedSearch
import com.diaryomi.ui.theme.GovFederalColor
import com.diaryomi.ui.theme.TceRnColor
import com.diaryomi.ui.theme.UnreadBadgeColor
import java.time.format.DateTimeFormatter

@Composable
fun SearchCard(
    search: TrackedSearch,
    onClick: () -> Unit,
    onEditTermsClick: () -> Unit,
    onEditDateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkAllAsReadClick: () -> Unit = {}
) {
    val extensionColor = when (search.extensionId) {
        "gov_federal" -> GovFederalColor
        "tce_rn" -> TceRnColor
        else -> MaterialTheme.colorScheme.primary
    }

    val hasUnread = search.unreadCount > 0
    val backgroundColor = if (hasUnread) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Faixa lateral colorida
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 110.dp)
                    .background(extensionColor)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top row: Extension badge + unread count + options menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = extensionColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = search.extensionName.ifEmpty { search.extensionId },
                            style = MaterialTheme.typography.labelSmall,
                            color = extensionColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = UnreadBadgeColor) {
                            Text("${search.unreadCount}")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Menu de opções (Editar / Excluir)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mais opções",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar termos de busca") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditTermsClick()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Alterar data inicial") },
                                leadingIcon = {
                                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditDateClick()
                                }
                            )

                            if (hasUnread) {
                                DropdownMenuItem(
                                    text = { Text("Marcar todas como lidas") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DoneAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onMarkAllAsReadClick()
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Text("Excluir busca", color = MaterialTheme.colorScheme.error)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Termos monitorados
                Text(
                    text = search.terms.joinToString(" • "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Rodapé de datas
                val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Desde ${search.startDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    search.lastCheckedDate?.let { lastChecked ->
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Última: ${lastChecked.format(shortDateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
