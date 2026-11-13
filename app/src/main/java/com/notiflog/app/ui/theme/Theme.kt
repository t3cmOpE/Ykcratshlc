package com.notiflog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun NotifLogTheme(content: @Composable () -> Unit) {
    val cs = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFFB69DFF), secondary = Color(0xFF8AB4FF), background = Color(0xFF111118),
        surface = Color(0xFF1B1B24), secondaryContainer = Color(0xFF2C2A45)
    ) else lightColorScheme(
        primary = Color(0xFF6D3BEA), secondary = Color(0xFF2563EB), background = Color(0xFFF6F5FB),
        surface = Color.White, secondaryContainer = Color(0xFFE6E0FF)
    )
    val t = Typography()
    MaterialTheme(
        colorScheme = cs,
        typography = t.copy(
            bodyLarge = t.bodyLarge.copy(fontSize = 17.sp), bodyMedium = t.bodyMedium.copy(fontSize = 16.sp),
            titleMedium = t.titleMedium.copy(fontSize = 18.sp)
        ),
        content = content
    )
}
