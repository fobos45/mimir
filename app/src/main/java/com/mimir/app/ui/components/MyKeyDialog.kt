package com.mimir.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyKeyDialog(
    pubkeyHex: String,
    ephemeralKeyHex: String? = null,   // null = трекер-режим, показываем только pubkey
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val directMode = ephemeralKeyHex != null

    // В режиме прямого подключения делимся "pubkey:ephemeral" как единой строкой
    val shareString = if (directMode && ephemeralKeyHex != null)
        "$pubkeyHex:$ephemeralKeyHex"
    else
        pubkeyHex

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(if (directMode) "Мои ключи" else "Мой публичный ключ") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (directMode)
                        "В режиме прямого подключения поделитесь обоими ключами"
                    else
                        "Отправьте этот ключ тому, кому хотите написать",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Постоянный ключ (всегда показываем)
                KeyBlock(
                    label = "Публичный ключ",
                    keyHex = pubkeyHex,
                )

                // Ephemeral ключ (только в режиме прямого подключения)
                if (directMode && ephemeralKeyHex != null) {
                    KeyBlock(
                        label = "Ephemeral ключ (Yggdrasil)",
                        keyHex = ephemeralKeyHex,
                        sublabel = "Меняется при перезапуске"
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Mimir key", shareString))
                            Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Копировать")
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT,
                                    if (directMode)
                                        "Мои ключи Mimir (прямое подключение):\n$shareString"
                                    else
                                        "Мой ключ Mimir:\n$shareString"
                                )
                                putExtra(Intent.EXTRA_SUBJECT, "Ключ Mimir")
                            }
                            context.startActivity(Intent.createChooser(intent, "Поделиться"))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Поделиться")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun KeyBlock(
    label: String,
    keyHex: String,
    sublabel: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(
                text = keyHex.chunked(8).joinToString(" "),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (sublabel != null) {
            Text(
                text  = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
