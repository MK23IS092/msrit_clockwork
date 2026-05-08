package com.vectorminds.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm
import kotlin.random.Random

/**
 * Tiny inline sparkline. If [points] is empty a deterministic seeded series
 * is rendered so cards don't look broken before live data is available.
 */
@Composable
fun VmSparkline(
    points: List<Float> = emptyList(),
    modifier: Modifier = Modifier.fillMaxWidth().height(32.dp),
    color: Color? = null,
    fillAlpha: Float = 0.18f,
    seed: Int = 7,
) {
    val vm = Vm.colors
    val tint = color ?: vm.brand
    val series = remember(points, seed) {
        if (points.isNotEmpty()) points
        else {
            val rng = Random(seed)
            List(20) { i ->
                0.4f + 0.5f * kotlin.math.sin(i * 0.55f).toFloat() + rng.nextFloat() * 0.15f
            }
        }
    }

    Canvas(modifier) {
        if (series.size < 2) return@Canvas
        val maxV = series.max()
        val minV = series.min()
        val range = (maxV - minV).coerceAtLeast(0.001f)
        val w = size.width
        val h = size.height
        val step = w / (series.size - 1)

        val path = Path()
        val fill = Path()
        series.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v - minV) / range) * (h - 2.dp.toPx()) - 1.dp.toPx()
            if (i == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, h)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(w, h)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(tint.copy(alpha = fillAlpha), Color.Transparent),
            ),
        )
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
