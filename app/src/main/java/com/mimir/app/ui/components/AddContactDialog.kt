package com.mimir.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddContactDialog(
    existingKeys: Set<String> = emptySet(),
    directMode: Boolean = false,   // true = прямое подключение, нужен pubkey:ephemeral
    onDismiss: () -> Unit,
    onAdd: (pubkeyHex: String, nickname: String, ephemeralKeyHex: String?) -> Unit,
) {
    var input    by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    // В режиме прямого подключения вход: "pubkey:ephemeral" (128+1+128 символов)
    // В обычном режиме: просто pubkey (64 символа)
    val parsed = remember(input) { parseInput(input.trim(), directMode) }
    val isDuplicate = parsed?.first != null && existingKeys.contains(parsed.first)
    val isValid = parsed != null && !isDuplicate && nickname.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(if (directMode) "Добавить контакт (прямое)" else "Новый контакт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = nickname,
                    onValueChange = { nickname = it },
                    label         = { Text("Имя контакта") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = input,
                    onValueChange = { input = it.trim().lowercase() },
                    label         = {
                        Text(if (directMode) "Ключи (pubkey:ephemeral)" else "Публичный ключ (64 символа)")
                    },
                    placeholder   = {
                        Text(if (directMode) "Вставьте строку вида ключ:ключ" else "64 hex символа")
                    },
                    singleLine    = false,
                    minLines      = if (directMode) 3 else 1,
                    maxLines      = if (directMode) 4 else 1,
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = input.isNotEmpty() && (parsed == null || isDuplicate),
                    supportingText = {
                        when {
                            isDuplicate      -> Text("Контакт уже добавлен",
                                color = MaterialTheme.colorScheme.error)
                            input.isNotEmpty() && parsed == null ->
                                Text(if (directMode)
                                    "Формат: <64 hex>:<64 hex>"
                                else
                                    "Ключ должен содержать ровно 64 символа")
                            else -> {
                                val len = if (directMode) {
                                    val p = input.indexOf(':')
                                    if (p >= 0) input.substring(0, p).length else input.length
                                } else input.length
                                Text("$len/64", color = if (parsed != null)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )

                if (directMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    ) {
                        Text(
                            text = "Попросите контакт нажать кнопку 🔑 и поделиться ключами через кнопку «Поделиться»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsed?.let { (pubkey, ephemeral) ->
                        onAdd(pubkey!!, nickname, ephemeral)
                    }
                    onDismiss()
                },
                enabled = isValid
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

/** Разбирает ввод пользователя.
 *  Обычный режим: "abcd...64символов" → Pair(pubkey, null)
 *  Прямой режим:  "pubkey:ephemeral"  → Pair(pubkey, ephemeral)
 */
private fun parseInput(input: String, directMode: Boolean): Pair<String?, String?>? {
    val hexChars = Regex("[0-9a-f]")
    if (!directMode) {
        if (input.length != 64) return null
        if (!input.all { it.isLetterOrDigit() }) return null
        return Pair(input, null)
    } else {
        // Формат pubkey:ephemeral или pubkey + ephemeral слитно (128 символов)
        val colonIdx = input.indexOf(':')
        return if (colonIdx == 64 && input.length == 129) {
            val pub = input.substring(0, 64)
            val eph = input.substring(65)
            if (pub.all { hexChars.matches(it.toString()) } &&
                eph.all { hexChars.matches(it.toString()) })
                Pair(pub, eph)
            else null
        } else if (input.length == 128 && input.all { hexChars.matches(it.toString()) }) {
            // Без разделителя — 128 символов подряд
            Pair(input.substring(0, 64), input.substring(64))
        } else null
    }
}
