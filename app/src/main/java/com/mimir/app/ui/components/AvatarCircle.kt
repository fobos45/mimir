package com.mimir.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val avatarColors = listOf(
    Color(0xFF7C6AF7), Color(0xFF00D9A3), Color(0xFFFF6B6B),
    Color(0xFF4FCBFF), Color(0xFFFFB347), Color(0xFFFF85C8),
)

@Composable
fun AvatarCircle(
    name: String,
    avatarPath: String? = null,   // зарезервировано на будущее
    size: Dp = 44.dp,
) {
    val color    = avatarColors[(name.firstOrNull()?.code ?: 0) % avatarColors.size]
    val initials = name.trim().split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = initials,
            fontSize   = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
            color      = color,
        )
    }
}
