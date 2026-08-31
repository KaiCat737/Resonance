package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.data.model.AppThemeMode
import com.example.data.model.AudioTrack

@Composable
fun ResonanceTheme(
    themeMode: AppThemeMode = AppThemeMode.FROSTED_GLASS,
    currentTrack: AudioTrack? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = DynamicThemeEngine.createColorScheme(themeMode, currentTrack)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ResonanceTheme(
        themeMode = AppThemeMode.FROSTED_GLASS,
        content = content
    )
}

