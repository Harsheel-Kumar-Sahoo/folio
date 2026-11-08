package com.folio.viewer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.folio.viewer.Kind

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF17171A),
    surface = Color.White,
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFEDEDF0),
    onSurfaceVariant = Color(0xFF6B6B76),
    outline = Color(0xFFDCDCE2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B87FF),
    onPrimary = Color(0xFF0B0B10),
    background = Color(0xFF0C0C0F),
    onBackground = Color(0xFFEDEDF0),
    surface = Color(0xFF16161B),
    onSurface = Color(0xFFEDEDF0),
    surfaceVariant = Color(0xFF202027),
    onSurfaceVariant = Color(0xFF9A9AA6),
    outline = Color(0xFF2C2C35)
)

@Composable
fun FolioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}

fun kindColor(kind: Kind): Color = when (kind) {
    Kind.PDF -> Color(0xFFE5484D)
    Kind.WORD -> Color(0xFF2F6FED)
    Kind.EXCEL -> Color(0xFF1F9D61)
    Kind.SLIDES -> Color(0xFFE8812A)
}
