package com.vectorminds.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.app.ui.theme.VmMotion

enum class VmButtonStyle { Primary, Secondary, Ghost, Danger }
enum class VmButtonSize  { Sm, Md, Lg }

@Composable
fun VmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: VmButtonStyle = VmButtonStyle.Primary,
    size: VmButtonSize = VmButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val vm = Vm.colors
    val height = when (size) {
        VmButtonSize.Sm -> 36.dp
        VmButtonSize.Md -> 48.dp
        VmButtonSize.Lg -> 56.dp
    }
    val hPad = when (size) {
        VmButtonSize.Sm -> 14.dp
        VmButtonSize.Md -> 18.dp
        VmButtonSize.Lg -> 22.dp
    }
    val (bgBrush, fg, border) = when (style) {
        VmButtonStyle.Primary -> Triple(
            Brush.linearGradient(listOf(vm.brand, vm.violet)),
            Color(0xFF0A1024),
            Color.Transparent,
        )
        VmButtonStyle.Secondary -> Triple(
            Brush.linearGradient(listOf(vm.surfaceElevated, vm.surfaceElevated)),
            vm.text,
            vm.borderStrong,
        )
        VmButtonStyle.Ghost -> Triple(
            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            vm.textMuted,
            vm.border,
        )
        VmButtonStyle.Danger -> Triple(
            Brush.linearGradient(listOf(vm.danger, vm.danger)),
            Color.White,
            Color.Transparent,
        )
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.97f else 1f,
        animationSpec = tween(VmMotion.FAST),
        label = "vmBtnScale",
    )
    val shape = RoundedCornerShape(Vm.shape.md)

    Row(
        modifier
            .scale(scale)
            .height(height)
            .clip(shape)
            .background(bgBrush, shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = hPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) fg else vm.textFaint) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            } else {
                if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}
