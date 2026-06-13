package com.mimir.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimir.app.data.Contact
import com.mimir.app.ui.components.AvatarCircle
import com.mimir.app.ui.components.MyKeyDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contacts: List<Contact>,
    onOpenChat: (String) -> Unit,
    onAddContact: () -> Unit,
    myPubkeyHex: String,
    connectionState: ConnectionState = ConnectionState(),
) {
    var showMyKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Индикатор статуса сети
                        NetworkIndicator(connectionState.status)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Mimir", style = MaterialTheme.typography.titleLarge)
                            if (connectionState.peerName.isNotEmpty()) {
                                Text(
                                    text  = connectionState.peerName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Кнопка "Мой ключ"
                    IconButton(onClick = { showMyKey = true }) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Мой ключ",
                            tint = MaterialTheme.colorScheme.primary
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
                onClick = onAddContact,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Добавить контакт",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            EmptyContactsPlaceholder(
                myPubkeyHex = myPubkeyHex,
                onShowKey = { showMyKey = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                )
            ) {
                items(contacts, key = { it.pubkeyHex }) { contact ->
                    ContactRow(contact = contact, onClick = { onOpenChat(contact.pubkeyHex) })
                }
            }
        }
    }

    if (showMyKey && myPubkeyHex.isNotEmpty()) {
        MyKeyDialog(
            pubkeyHex = myPubkeyHex,
            onDismiss = { showMyKey = false }
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AvatarCircle(name = contact.nickname, avatarPath = contact.avatarPath, size = 50.dp)
                if (contact.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (contact.isOnline) "онлайн"
                           else if (contact.lastSeen > 0) "был(а) ${formatLastSeen(contact.lastSeen)}"
                           else "офлайн",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (contact.isOnline) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(8.dp))

            if (contact.unreadCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        text = if (contact.unreadCount > 99) "99+" else contact.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContactsPlaceholder(
    myPubkeyHex: String,
    onShowKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Нет контактов",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Нажмите + чтобы добавить первый",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        if (myPubkeyHex.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onShowKey) {
                Icon(Icons.Default.Key, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Показать мой ключ")
            }
        }
    }
}

private fun formatLastSeen(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000     -> "только что"
        diff < 3_600_000  -> "${diff / 60_000} мин назад"
        diff < 86_400_000 -> "${diff / 3_600_000} ч назад"
        else -> SimpleDateFormat("d MMM", Locale("ru")).format(Date(ts))
    }
}

@Composable
private fun NetworkIndicator(status: NetworkStatus) {
    val color = when (status) {
        NetworkStatus.ONLINE     -> Color(0xFF00D9A3)   // зелёный
        NetworkStatus.CONNECTING -> Color(0xFFFFB347)   // жёлтый
        NetworkStatus.OFFLINE    -> Color(0xFFFF6B6B)   // красный
    }

    // Пульсация для состояния "подключение"
    val alpha by if (status == NetworkStatus.CONNECTING) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue  = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation  = androidx.compose.animation.core.tween(800),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
