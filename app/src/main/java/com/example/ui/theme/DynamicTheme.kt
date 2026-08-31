package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode
import com.example.data.model.AudioTrack

data class DynamicPlayerPalette(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color
)

object DynamicThemeEngine {
    fun createColorScheme(
        themeMode: AppThemeMode,
        currentTrack: AudioTrack?
    ): ColorScheme {
        return when (themeMode) {
            AppThemeMode.FROSTED_GLASS -> {
                val primaryColor = if (currentTrack != null) Color(currentTrack.albumSecondaryColorHex) else Color(0xFF6200EE)
                darkColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC6),
                    tertiary = Color(0xFF00E5FF),
                    background = Color(0xFF0D1117),
                    surface = Color(0xFF121418),
                    surfaceVariant = Color(0xFF1C2028),
                    onBackground = Color(0xFFE1E2E6),
                    onSurface = Color(0xFFE1E2E6),
                    onSurfaceVariant = Color(0x99E1E2E6),
                    onPrimary = Color.White
                )
            }

            AppThemeMode.ALBUM_DYNAMIC -> {
                val primaryColor = if (currentTrack != null) Color(currentTrack.albumSecondaryColorHex) else Color(0xFF00E5FF)
                val albumColor = if (currentTrack != null) Color(currentTrack.albumColorHex) else Color(0xFF1E293B)
                val bgColor = Color(0xFF0A0D14)

                darkColorScheme(
                    primary = primaryColor,
                    secondary = primaryColor.copy(alpha = 0.8f),
                    tertiary = if (currentTrack != null) Color(currentTrack.format.badgeColor) else Color(0xFFFFB300),
                    background = bgColor,
                    surface = Color(0xFF111726),
                    surfaceVariant = Color(0xFF1B2338),
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFF1F5F9),
                    onPrimary = Color.Black
                )
            }

            AppThemeMode.OBSIDIAN_OLED -> {
                darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    secondary = Color(0xFFB388FF),
                    tertiary = Color(0xFFFFB300),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0D0D0D),
                    surfaceVariant = Color(0xFF171717),
                    onBackground = Color(0xFFEDEDED),
                    onSurface = Color(0xFFEDEDED),
                    onPrimary = Color.Black
                )
            }

            AppThemeMode.NEON_CYBER -> {
                darkColorScheme(
                    primary = Color(0xFFFF007F), // Neon Pink
                    secondary = Color(0xFF00F5D4), // Cyber Teal
                    tertiary = Color(0xFFFEE440),
                    background = Color(0xFF080711),
                    surface = Color(0xFF120E24),
                    surfaceVariant = Color(0xFF1D173B),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFFFFFFF),
                    onPrimary = Color.White
                )
            }

            AppThemeMode.STUDIO_AMBER -> {
                darkColorScheme(
                    primary = Color(0xFFFFB300), // Vintage Studio Amber
                    secondary = Color(0xFFFFD54F),
                    tertiary = Color(0xFFFF7043),
                    background = Color(0xFF120E09),
                    surface = Color(0xFF1C1710),
                    surfaceVariant = Color(0xFF2B2319),
                    onBackground = Color(0xFFFFF8E1),
                    onSurface = Color(0xFFFFF8E1),
                    onPrimary = Color.Black
                )
            }

            AppThemeMode.MINIMAL_SLATE -> {
                darkColorScheme(
                    primary = Color(0xFF64748B),
                    secondary = Color(0xFF94A3B8),
                    tertiary = Color(0xFF38BDF8),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    surfaceVariant = Color(0xFF334155),
                    onBackground = Color(0xFFF8FAFC),
                    onSurface = Color(0xFFF8FAFC),
                    onPrimary = Color.White
                )
            }
        }
    }
}
