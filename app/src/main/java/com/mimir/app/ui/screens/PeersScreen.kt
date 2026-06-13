package com.mimir.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeersScreen(
    peers: List<YggPeer>,
    onBack: () -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onAdd: (address: String, label: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onRestartService: () -> Unit = {},
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                title = {
                    Column {
                        Text("Управление пирами")
                        Text(
                            text  = "${peers.count { it.enabled }} из ${peers.size} активны",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { showAddDialog = true },
                containerColor   = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, "Добавить пир",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
                start  = 16.dp,
                end    = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок встроенных пиров
            item {
                Text(
                    text     = "Официальные пиры",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            items(peers.filter { it.isDefault }, key = { it.id }) { peer ->
                PeerRow(
                    peer      = peer,
                    onToggle  = { onToggle(peer.id, it) },
                    onRemove  = null,  // встроенные нельзя удалить
                )
            }

            // Пользовательские пиры
            val custom = peers.filter { !it.isDefault }
            if (custom.isNotEmpty()) {
                item {
                    Text(
                        text     = "Свои пиры",
                        style    = MaterialTheme.typography.labelLarge,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(custom, key = { it.id }) { peer ->
                    PeerRow(
                        peer     = peer,
                        onToggle = { onToggle(peer.id, it) },
                        onRemove = { onRemove(peer.id) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text  = "Изменения вступят в силу после перезапуска подключения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick  = { onRestartService(); onBack() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Применить и переподключиться")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPeerDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { addr, label -> onAdd(addr, label); showAddDialog = false }
        )
    }
}

@Composable
private fun PeerRow(
    peer: YggPeer,
    onToggle: (Boolean) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (peer.enabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = if (peer.enabled) 2.dp else 0.dp,
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Иконка статуса
            Icon(
                imageVector  = if (peer.enabled) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint         = if (peer.enabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier     = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))

            // Название и адрес
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = peer.label,
                    style    = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = if (peer.enabled) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text     = peer.address,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (peer.enabled) 1f else 0.4f
                    )
                )
            }

            // Кнопка удаления (только для кастомных)
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить",
                        tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            // Тумблер
            Switch(
                checked         = peer.enabled,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor       = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor       = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor     = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
        }
    }
}

@Composable
private fun AddPeerDialog(
    onDismiss: () -> Unit,
    onAdd: (address: String, label: String) -> Unit,
) {
    var address by remember { mutableStateOf("tcp://") }
    var label   by remember { mutableStateOf("") }
    val isValid = address.startsWith("tcp://") && address.length > 10

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        title            = { Text("Добавить пир") },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = address,
                    onValueChange = { address = it.trim() },
                    label         = { Text("Адрес пира") },
                    placeholder   = { Text("tcp://hostname:7743") },
                    singleLine    = true,
                    isError       = address.isNotBlank() && !isValid,
                    supportingText = {
                        if (address.isNotBlank() && !isValid)
                            Text("Формат: tcp://hostname:port")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text("Название (необязательно)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onAdd(address, label) },
                enabled  = isValid,
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
