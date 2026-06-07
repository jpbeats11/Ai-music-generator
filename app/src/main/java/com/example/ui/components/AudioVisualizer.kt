package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrightMagenta
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    amplitudes: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val wavePhaseAngle by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("audio_visualizer_canvas")
        ) {
            val count = amplitudes.size.coerceAtLeast(1)
            val width = size.width
            val height = size.height

            // 1. Draw flowing sine curve
            val avgPower = amplitudes.average().toFloat()
            val path = Path()
            val segments = 100
            val waveAmplitude = (avgPower * height * 0.45f).coerceIn(4f, height * 0.45f)
            
            path.moveTo(0f, height / 2f)
            for (xStep in 0..segments) {
                val fraction = xStep.toFloat() / segments
                val x = fraction * width
                val sineDisp = sin((fraction * 10f) + wavePhaseAngle)
                val y = (height / 2f) + sineDisp * waveAmplitude
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(CyberCyan, ElectricViolet, BrightMagenta)
                ),
                style = Stroke(width = 3.dp.toPx())
            )

            // 2. Draw amplitude bars
            val spacing = 5.dp.toPx()
            val totalSpacing = spacing * (count - 1)
            val barWidth = (width - totalSpacing) / count

            for (i in 0 until count) {
                val originalAmp = amplitudes[i]
                val minBarHeight = 3.dp.toPx()
                
                val adjustedAmp = if (isPlaying) {
                    (originalAmp * 0.82f + (sin(wavePhaseAngle * 3f + i) * 0.08f)).coerceIn(0.01f, 1f)
                } else {
                    0.05f
                }
                
                val barHeight = adjustedAmp * height * 0.85f
                val finalBarHeight = barHeight.coerceAtLeast(minBarHeight)

                val x = i * (barWidth + spacing)
                val y = (height - finalBarHeight) / 2f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ElectricViolet, CyberCyan, BrightMagenta)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, finalBarHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
