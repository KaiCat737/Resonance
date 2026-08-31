package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Frosted Glass Core Colors
val FrostedBackground = Color(0xFF0D1117)
val FrostedSurface = Color(0xFF121418)
val FrostedGlassWhite = Color(0x14FFFFFF) // 8% white glass
val FrostedGlassWhiteHeavy = Color(0x22FFFFFF) // 13% white glass
val FrostedGlassBorder = Color(0x1AFFFFFF) // 10% white crisp border
val FrostedGlassBorderHeavy = Color(0x33FFFFFF) // 20% white crisp border
val FrostedGlassBorderAccent = Color(0x4D00E5FF) // subtle cyan edge
val FrostedTextPrimary = Color(0xFFE1E2E6)
val FrostedTextSecondary = Color(0x99E1E2E6)
val FrostedTextMuted = Color(0x66E1E2E6)

val FrostedPurple = Color(0xFF6200EE)
val FrostedCyan = Color(0xFF03DAC6)
val FrostedNeonCyan = Color(0xFF00E5FF)

/**
 * Ambient Frosted Glass Background canvas drawing the atmospheric deep slate
 * with soft top-left purple glow and bottom-right cyan ambient light.
 */
@Composable
fun FrostedGlassAmbientBackground(
    modifier: Modifier = Modifier,
    topGlowColor: Color = FrostedPurple,
    bottomGlowColor: Color = FrostedCyan,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base deep canvas
            drawRect(color = FrostedBackground)

            // Top-left atmospheric purple ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        topGlowColor.copy(alpha = 0.22f),
                        topGlowColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.1f, height * 0.05f),
                    radius = width * 0.9f
                ),
                radius = width * 0.9f,
                center = Offset(width * 0.1f, height * 0.05f)
            )

            // Bottom-right atmospheric cyan ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bottomGlowColor.copy(alpha = 0.16f),
                        bottomGlowColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.9f, height * 0.85f),
                    radius = width * 0.85f
                ),
                radius = width * 0.85f,
                center = Offset(width * 0.9f, height * 0.85f)
            )

            // Center subtle radiant sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.45f),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(width * 0.5f, height * 0.45f)
            )
        }

        content()
    }
}

/**
 * Convenience modifier to give any composable a Frosted Glass appearance.
 */
fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = FrostedGlassWhite,
    borderColor: Color = FrostedGlassBorder,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 0.dp,
    shadowColor: Color = Color.Black
): Modifier = this
    .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape, spotColor = shadowColor) else Modifier)
    .clip(shape)
    .border(borderWidth, borderColor, shape)
