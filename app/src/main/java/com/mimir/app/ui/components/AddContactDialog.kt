package com.mimir.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (pubkeyHex: String, nickname: String) -> Unit,
) {
    var pubkey   by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    val isValid  = pubkey.length == 64 && pubkey.all { it.isLetterOrDigit() } && nickname.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Новый контакт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pubkey,
                    onValueChange = { pubkey = it.trim() },
                    label = { Text("Публичный ключ (64 символа)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = pubkey.isNotEmpty() && pubkey.length != 64,
                    supportingText = {
                        if (pubkey.isNotEmpty() && pubkey.length != 64)
                            Text("Ключ должен быть 64 символа")
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
