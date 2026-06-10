package com.mimir.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimir.app.data.Contact
import com.mimir.app.ui.components.AvatarCircle
import uniffi.mimir.CallStatus

@Composable
fun CallScreen(
    contact: Contact?,
    status: CallStatus,
    duration: Long,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangup: () -> Unit,
) {
    // Пульсирующий фон для активного звонка
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface,
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(60.dp))

            // Аватар с пульсацией при звонке
            Box(contentAlignment = Alignment.Center) {
                if (status == CallStatus.IN_CALL) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    )
                }
                AvatarCircle(
                    name = contact?.nickname ?: "?",
                    avatarPath = contact?.avatarPath,
                    size = 110.dp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = contact?.nickname ?: "Неизвестный",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Статус
            Text(
                text = when (status) {
                    CallStatus.CALLING   -> "Вызов…"
                    CallStatus.RECEIVING -> "Входящий звонок"
                    CallStatus.IN_CALL   -> formatDuration(duration)
                    CallStatus.HANGUP    -> "Завершён"
                    CallStatus.IDLE      -> ""
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.weight(1f))

            // Кнопки управления
            when (status) {
                CallStatus.RECEIVING -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallButton(
                            icon = Icons.Default.CallEnd,
                            label = "Отклонить",
                            color = MaterialTheme.colorScheme.error,
                            onClick = onDecline
                        )
                        CallButton(
                            icon = Icons.Default.Call,
                            label = "Ответить",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = onAnswer
                        )
                    }
                }
                CallStatus.CALLING, CallStatus.IN_CALL -> {
                    CallButton(
                        icon = Icons.Default.CallEnd,
                        label = "Завершить",
                        color = MaterialTheme.colorScheme.error,
                        onClick = onHangup
                    )
                }
                else -> {}
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun CallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            Icon(icon, contentDescription = label,
                tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
