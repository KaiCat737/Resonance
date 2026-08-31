package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.ui.theme.FrostedCyan
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedNeonCyan

@Composable
fun HiResBadge(
    track: AudioTrack,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val formatColor = Color(track.format.badgeColor)

    if (compact) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33000000))
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (track.isHiRes) {
                    PulsingDot(color = FrostedNeonCyan, size = 5)
                }
                Text(
                    text = "${track.format.displayName} ${track.bitDepth}b",
                    color = if (track.isHiRes) FrostedNeonCyan else formatColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PulsingDot(color = if (track.isHiRes) FrostedNeonCyan else formatColor, size = 6)

            Icon(
                imageVector = if (track.isHiRes) Icons.Default.Hd else Icons.Default.GraphicEq,
                contentDescription = "Hi-Res Audio",
                tint = if (track.isHiRes) FrostedNeonCyan else formatColor,
                modifier = Modifier.size(15.dp)
            )

            Text(
                text = track.hiResFormattedTag,
                color = Color(0xFFE1E2E6),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun LosslessPill(
    format: AudioFormat,
    isHiRes: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeColor = if (isHiRes) FrostedNeonCyan else Color(format.badgeColor)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x40000000))
            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isHiRes) {
            PulsingDot(color = FrostedNeonCyan, size = 5)
        }
        Text(
            text = if (isHiRes) "HI-RES ${format.displayName}" else format.displayName,
            color = if (isHiRes) Color(0xFFD0F8FF) else badgeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun PulsingDot(
    color: Color = FrostedNeonCyan,
    size: Int = 6,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
