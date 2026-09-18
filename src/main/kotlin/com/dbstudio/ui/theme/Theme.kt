package com.dbstudio.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand & Neutral Colors (Light Theme) ────────────────────────────

val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo50 = Color(0xFFEEF2FF)
val Indigo100 = Color(0xFFE0E7FF)

val Sky600 = Color(0xFF0284C7)
val Sky100 = Color(0xFFE0F2FE)

val Emerald600 = Color(0xFF059669)
val Emerald100 = Color(0xFFD1FAE5)

val Amber600 = Color(0xFFD97706)
val Amber100 = Color(0xFFFEF3C7)

val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val Red600 = Color(0xFFDC2626)
val Red100 = Color(0xFFFEE2E2)

// ── Light Color Scheme ──────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo50,
    onPrimaryContainer = Indigo700,

    secondary = Sky600,
    onSecondary = Color.White,
    secondaryContainer = Sky100,
    onSecondaryContainer = Color(0xFF0369A1),

    tertiary = Emerald600,
    onTertiary = Color.White,
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Color(0xFF065F46),

    background = Slate50,
    onBackground = Slate900,

    surface = Color.White,
    onSurface = Slate900,

    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Slate300,

    outline = Slate300,
    outlineVariant = Slate200,

    error = Red600,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Color(0xFF991B1B),
)

// ── Dark Color Scheme (Google Cloud / Material 3 Dark) ──────────────

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC), // Indigo 300
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),

    secondary = Color(0xFF7DD3FC), // Sky 300
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),

    tertiary = Color(0xFF6EE7B7), // Emerald 300
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),

    background = Color(0xFF0B0F19), // Deep dark
    onBackground = Color(0xFFF1F5F9),

    surface = Color(0xFF131B2E),
    onSurface = Color(0xFFF1F5F9),

    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),

    surfaceContainerLowest = Color(0xFF080C14),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainer = Color(0xFF162036),
    surfaceContainerHigh = Color(0xFF1E2B47),
    surfaceContainerHighest = Color(0xFF28385A),

    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),

    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

// ── Theme Composable ────────────────────────────────────────────────

@Composable
fun DBStudioTheme(
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content
    )
}

// ── Utility colors ──────────────────────────────────────────────────

object AppColors {
    val primaryKey = Color(0xFFD97706)       // Amber for PK
    val foreignKey = Color(0xFF0284C7)       // Sky blue for FK
    val unique = Color(0xFF7C3AED)           // Purple/Violet for unique
    val autoIncrement = Color(0xFF059669)    // Emerald for autoincrement
    val nullable = Color(0xFF64748B)         // Slate for nullable
    val entityHeader = Color(0xFF4F46E5)     // Indigo for entity header
    val entityBody = Color.White             // Pure white for entity body
    val relationLine = Color(0xFF4F46E5)     // Indigo for relation lines
    val selectedBorder = Color(0xFF4F46E5)   // Indigo for selected items
    val diagramGrid = Color(0xFFE2E8F0)      // Subtle soft slate grid
}
