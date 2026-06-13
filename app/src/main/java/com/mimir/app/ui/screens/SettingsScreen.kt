package com.mimir.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    useTracker: Boolean,
    onUseTrackerChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenPeers: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                title = { Text("Настройки") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsSectionLabel("Сеть")

            SettingsRow(
                icon      = Icons.Default.Hub,
                title     = "Управление пирами",
                subtitle  = "Yggdrasil bootstrap-узлы",
                onClick   = onOpenPeers,
            )

            Spacer(Modifier.height(4.dp))

            // Переключатель режима подключения
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (useTracker) Icons.Default.CloudSync else Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = if (useTracker) "Через трекер" else "Прямое подключение",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text  = if (useTracker)
                                    "Трекер помогает находить контакты"
                                else
                                    "Постоянный адрес, без трекера",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = useTracker,
                            onCheckedChange = onUseTrackerChange,
                            colors          = SwitchDefaults.colors(
                                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(10.dp))

                    // Пояснение текущего режима
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (useTracker)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(top = 1.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (useTracker)
                                    "Трекер используется для поиска контактов " +
                                    "по сети. Рекомендуется для большинства случаев."
                                else
                                    "Оба устройства должны быть подключены к одному " +
                                    "Yggdrasil-пиру. Трекер не требуется.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text  = "⚠ Изменение режима требует перезапуска подключения",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel("О приложении")

            SettingsRow(
                icon      = Icons.Default.Info,
                title     = "Mimir",
                subtitle  = "Защищённый P2P мессенджер",
                onClick   = {},
                showArrow = false,
            )
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = true,
) {
    Surface(
        onClick   = onClick,
        shape     = RoundedCornerShape(12.dp),
        color     = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showArrow) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
