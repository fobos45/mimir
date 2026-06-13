package com.mimir.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AddContactDialog(
    existingKeys: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onAdd: (pubkeyHex: String, nickname: String) -> Unit,
) {
    var pubkey   by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    val isDuplicate = pubkey.length == 64 && existingKeys.contains(pubkey.lowercase())
    val isValidKey  = pubkey.length == 64 && pubkey.all { it.isLetterOrDigit() } && !isDuplicate
    val isValid     = isValidKey && nickname.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Новый контакт") },
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
                    value         = pubkey,
                    onValueChange = { pubkey = it.trim().lowercase() },
                    label         = { Text("Публичный ключ (64 символа)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = pubkey.isNotEmpty() && (pubkey.length != 64 || isDuplicate),
                    supportingText = {
                        when {
                            isDuplicate              -> Text("Этот контакт уже добавлен",
                                color = MaterialTheme.colorScheme.error)
                            pubkey.isNotEmpty() && pubkey.length != 64 ->
                                Text("Ключ должен содержать ровно 64 символа")
                            pubkey.isNotEmpty() && !pubkey.all { it.isLetterOrDigit() } ->
                                Text("Ключ содержит недопустимые символы")
                            else -> Text("${pubkey.length}/64",
                                color = if (pubkey.length == 64)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(pubkey, nickname); onDismiss() },
                enabled = isValid
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
