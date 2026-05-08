package com.vectorminds.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.app.ui.theme.VmMotion

/**
 * Animated arc with the percentage in the middle. Used for emergence /
 * novelty / impact / confidence on trend cards and detail screens.
 */
@Composable
fun VmScoreRing(
    progress: Float,                  // 0f..1f
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 5.dp,
    accent: Color? = null,
) {
    val vm = Vm.colors
    val tint = accent ?: when {
        progress >= 0.7f -> vm.scoreHigh
        progress >= 0.4f -> vm.scoreMid
        else -> vm.scoreLow
    }
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(VmMotion.SLOW),
        label = "vmScoreRing",
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val sw = strokeWidth.toPx()
                val arcSize = Size(this.size.width - sw, this.size.height - sw)
                val topLeft = Offset(sw / 2f, sw / 2f)

                drawArc(
                    color = vm.borderStrong,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
            Text(
                "${(animated * 100).toInt()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = vm.text,
            )
        }
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = vm.textFaint,
        )
    }
}
