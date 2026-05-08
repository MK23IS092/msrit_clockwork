package com.vectorminds.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.app.ui.theme.VmMotion

/**
 * Single KPI tile used across Dashboard / Trends.
 * Small uppercase eyebrow label + big numeric value + optional sublabel.
 */
@Composable
fun VmStat(
    label: String,
    value: String,
    icon: ImageVector? = null,
    accent: Color? = null,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
) {
    val vm = Vm.colors
    val tint = accent ?: vm.brand
    VmCard(modifier = modifier, contentPadding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(tint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = vm.textFaint,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = vm.text,
            )
            if (sublabel != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = vm.textMuted,
                )
            }
        }
    }
}

/**
 * Thin progress bar with rounded ends and gradient fill.
 * Drawn directly on Canvas for crispness and animation.
 */
@Composable
fun VmProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color? = null,
    trackColor: Color? = null,
) {
    val vm = Vm.colors
    val tint = color ?: vm.brand
    val track = trackColor ?: vm.borderStrong
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(VmMotion.SLOW),
        label = "vmProgress",
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2f
        // track
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(r, r),
        )
        // fill
        if (animated > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(tint, tint.copy(alpha = 0.7f))),
                topLeft = Offset.Zero,
                size = Size(width = size.width * animated, height = size.height),
                cornerRadius = CornerRadius(r, r),
            )
        }
    }
}
