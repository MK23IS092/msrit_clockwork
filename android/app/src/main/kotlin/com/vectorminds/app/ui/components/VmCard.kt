package com.vectorminds.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.app.ui.theme.VmMotion

/**
 * Premium card primitive.
 *
 * Important: the content slot is a [ColumnScope] receiver, so any number of
 * sibling composables you pass automatically stack vertically. (An earlier
 * version put content in a [Box], which silently stacked siblings on top of
 * each other and broke layout — never do that for a card primitive.)
 *
 * @param accent  if non-null, paints a 2dp gradient bar across the very top
 *                edge of the card. Use sparingly to flag "live" / "primary"
 *                tiles.
 */
@Composable
fun VmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    cornerRadius: Dp = Vm.shape.lg,
    elevated: Boolean = false,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val vm = Vm.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(VmMotion.FAST),
        label = "vmCardPressScale",
    )

    val container = if (elevated) vm.surfaceElevated else vm.surface
    val border = if (elevated) vm.borderStrong else vm.border
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(container)
            .border(BorderStroke(1.dp, border), shape)
            .let { mod ->
                if (onClick != null) {
                    mod.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else mod
            },
    ) {
        if (accent != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = 0f),
                                accent.copy(alpha = 0.7f),
                                accent.copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
        }
        Column(modifier = Modifier.padding(contentPadding)) { content() }
    }
}
