package com.vic.inkflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ColorPickerDialog(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var alpha by remember { mutableFloatStateOf(1f) }

    val pickedColor = Color.hsv(hue, saturation, brightness, alpha)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇顏色") },
        confirmButton = {
            TextButton(onClick = { onColorSelected(pickedColor); onDismiss() }) {
                Text("確認")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Hue ring
                HueRing(
                    hue = hue,
                    modifier = Modifier.size(200.dp),
                    onHueChanged = { hue = it }
                )
                Spacer(Modifier.height(16.dp))
                // Saturation slider
                Text("飽和度", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                // Brightness slider
                Text("明度", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                // Opacity (alpha) slider
                Text("不透明度  ${(alpha * 100).toInt()}%", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                // Preview (checkerboard behind for alpha visibility)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // Checkerboard for transparency visualization
                    val cellSize = 10.dp.toPx()
                    val cols = (size.width / cellSize).toInt() + 1
                    val rows = (size.height / cellSize).toInt() + 1
                    for (row in 0..rows) {
                        for (col in 0..cols) {
                            drawRect(
                                color = if ((row + col) % 2 == 0) Color.LightGray else Color.White,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                    drawRect(color = pickedColor, size = size)
                }
            }
        }
    )
}

@Composable
private fun HueRing(
    hue: Float,
    modifier: Modifier,
    onHueChanged: (Float) -> Unit
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val cx = size.width / 2f
                val cy = size.height / 2f
                val angle = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())).toFloat()
                onHueChanged((angle + 360f) % 360f)
            }
        }
    ) {
        drawHueRing()
        // Draw selector dot
        val radius = min(size.width, size.height) / 2f * 0.75f
        val angleRad = Math.toRadians(hue.toDouble())
        val cx = size.width / 2f + radius * cos(angleRad).toFloat()
        val cy = size.height / 2f + radius * sin(angleRad).toFloat()
        drawCircle(color = Color.White, radius = 10f, center = Offset(cx, cy), style = Stroke(width = 3f))
        drawCircle(color = Color.hsv(hue, 1f, 1f), radius = 10f, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawHueRing() {
    val ringThickness = min(size.width, size.height) * 0.12f
    val outerRadius = min(size.width, size.height) / 2f
    val steps = 360
    for (i in 0 until steps) {
        val startAngle = i.toFloat()
        val sweepAngle = 1.2f
        val color = Color.hsv(i.toFloat(), 1f, 1f)
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(size.width / 2f - outerRadius, size.height / 2f - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = ringThickness)
        )
    }
}
