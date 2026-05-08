package com.vectorminds.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm

/**
 * Shimmer-able placeholder rectangle for skeleton loaders.
 * Cheap (single infinite transition driving a horizontal gradient).
 */
@Composable
fun VmShimmerBox(
    modifier: Modifier = Modifier.fillMaxWidth().height(20.dp),
    cornerRadius: Dp = 8.dp,
) {
    val vm = Vm.colors
    val transition = rememberInfiniteTransition(label = "vmShimmer")
    val x by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vmShimmerX",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            vm.surfaceElevated,
            vm.surfaceElevated.copy(alpha = 0.6f),
            vm.surfaceElevated,
        ),
        start = Offset(x, 0f),
        end = Offset(x + 200f, 0f),
    )
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}
