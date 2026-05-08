package com.vectorminds.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm

/** Chip variants used across the redesign. */
enum class VmChipStyle { Soft, Outline, Solid }

/**
 * Refined chip: smaller, semantic colors, never a full-saturation background.
 *
 * Soft   → [tint] tinted background + [tint] text   (the default)
 * Outline → transparent + [tint] border + muted text
 * Solid  → [tint] background + onColor text (use sparingly)
 */
@Composable
fun VmChip(
    text: String,
    modifier: Modifier = Modifier,
    style: VmChipStyle = VmChipStyle.Soft,
    tint: Color? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val vm = Vm.colors
    val accent = tint ?: vm.brand
    val (bg, fg, br) = when (style) {
        VmChipStyle.Soft   -> Triple(accent.copy(alpha = 0.12f), accent, Color.Transparent)
        VmChipStyle.Outline -> Triple(Color.Transparent, vm.textMuted, vm.border)
        VmChipStyle.Solid  -> Triple(accent, vm.background, Color.Transparent)
    }
    val shape = RoundedCornerShape(Vm.shape.sm)

    val rowMod = modifier
        .clip(shape)
        .background(bg, shape)
        .border(BorderStroke(1.dp, br), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(PaddingValues(horizontal = 10.dp, vertical = 5.dp))

    Row(rowMod, verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg,
            )
        }
    }
}

/**
 * Selectable filter chip for filter bars (Trends, Pipelines).
 * Animated background shift on selection.
 */
@Composable
fun VmFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val vm = Vm.colors
    val shape = RoundedCornerShape(Vm.shape.sm)

    val bg = when {
        !enabled -> Color.Transparent
        selected -> vm.brand.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> vm.textFaint
        selected -> vm.brand
        else -> vm.textMuted
    }
    val border = when {
        !enabled -> vm.border
        selected -> vm.brand.copy(alpha = 0.55f)
        else -> vm.border
    }

    Row(
        modifier
            .clip(shape)
            .background(bg, shape)
            .border(BorderStroke(1.dp, border), shape)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}
