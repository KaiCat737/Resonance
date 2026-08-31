package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.VisualizerStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpectrumVisualizer(
    spectrumData: FloatArray,
    style: VisualizerStyle,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    isPlaying: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height

        when (style) {
            VisualizerStyle.BARS -> {
                val barCount = 16
                val totalSpacing = width * 0.25f
                val barWidth = (width - totalSpacing) / barCount
                val spacing = totalSpacing / (barCount - 1)

                for (i in 0 until barCount) {
                    val magnitude = (spectrumData.getOrElse(i) { 0.1f }).coerceIn(0.05f, 1.0f)
                    val barHeight = (canvasHeight * 0.9f * magnitude).coerceAtLeast(4f)
                    val x = i * (barWidth + spacing)
                    val y = canvasHeight - barHeight

                    // Bar gradient
                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor,
                            secondaryColor,
                            primaryColor.copy(alpha = 0.4f)
                        ),
                        startY = y,
                        endY = canvasHeight
                    )

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Cap light
                    if (isPlaying && magnitude > 0.25f) {
                        drawCircle(
                            color = primaryColor,
                            radius = barWidth * 0.45f,
                            center = Offset(x + barWidth / 2f, (y - 3f).coerceAtLeast(3f))
                        )
                    }
                }
            }

            VisualizerStyle.LIQUID -> {
                val path = Path()
                path.moveTo(0f, canvasHeight / 2f)

                val points = 32
                for (i in 0..points) {
                    val x = (width / points) * i
                    val specIdx = (i / 2).coerceIn(0, spectrumData.size - 1)
                    val amp = spectrumData[specIdx] * canvasHeight * 0.4f
                    val wave = sin((i.toFloat() / points) * 4 * PI.toFloat() + phase) * amp
                    val y = (canvasHeight / 2f) + wave
                    if (i == 0) path.moveTo(x, y.toFloat()) else path.lineTo(x, y.toFloat())
                }

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Fill underneath with subtle glow
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, canvasHeight)
                    lineTo(0f, canvasHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                        startY = canvasHeight / 2f,
                        endY = canvasHeight
                    )
                )
            }

            VisualizerStyle.OSCILLOSCOPE -> {
                val path = Path()
                val points = 64
                for (i in 0..points) {
                    val normX = i.toFloat() / points
                    val x = normX * width
                    val band = (normX * (spectrumData.size - 1)).toInt()
                    val mag = spectrumData.getOrElse(band) { 0.1f }
                    val wave = sin(normX * 8 * PI.toFloat() + phase * 2f) * (mag * canvasHeight * 0.45f)
                    val y = (canvasHeight / 2f) + wave
                    if (i == 0) path.moveTo(x, y.toFloat()) else path.lineTo(x, y.toFloat())
                }

                // Laser glow stroke
                drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = 0.4f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = path,
                    color = secondaryColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            VisualizerStyle.VINYL_GROOVE -> {
                val centerX = width / 2f
                val centerY = canvasHeight / 2f
                val maxRadius = (canvasHeight / 2f).coerceAtMost(width / 2f) * 0.95f

                val avgBass = (spectrumData.take(4).average().toFloat()).coerceIn(0.1f, 1f)
                val avgTreble = (spectrumData.takeLast(4).average().toFloat()).coerceIn(0.1f, 1f)

                // Concentric circles with wave distortion
                for (ring in 1..4) {
                    val baseRadius = (maxRadius / 4f) * ring
                    val modRadius = baseRadius + (if (ring % 2 == 0) avgBass else avgTreble) * 6f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(secondaryColor, primaryColor),
                            center = Offset(centerX, centerY),
                            radius = modRadius
                        ),
                        radius = modRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }
    }
}
