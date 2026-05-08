package com.vectorminds.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.app.ui.theme.VmMotion

data class VmDockItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Floating glass-style nav dock. Selected item expands inline to show its
 * label (Arc / Vercel pattern) — others stay icon-only to save space.
 *
 * Anchor this with `Modifier.align(Alignment.BottomCenter)` inside a Box.
 */
@Composable
fun VmNavDock(
    items: List<VmDockItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = Vm.colors
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .clip(shape)
            .background(vm.surfaceElevated.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, vm.border), shape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            VmDockButton(
                item = item,
                selected = item.key == selectedKey,
                onClick = { onSelect(item.key) },
            )
        }
    }
}

@Composable
private fun VmDockButton(
    item: VmDockItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val vm = Vm.colors
    val pad by animateDpAsState(
        targetValue = if (selected) 14.dp else 12.dp,
        animationSpec = tween(VmMotion.MEDIUM),
        label = "dockPad",
    )
    val shape = RoundedCornerShape(16.dp)
    val bg = if (selected) vm.brand.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent
    val tint = if (selected) vm.brand else vm.textMuted

    Row(
        Modifier
            .clip(shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = pad, vertical = 10.dp)
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(20.dp))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(VmMotion.MEDIUM)) +
                expandHorizontally(tween(VmMotion.MEDIUM)),
            exit = fadeOut(tween(VmMotion.FAST)) +
                shrinkHorizontally(tween(VmMotion.FAST)),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = tint,
                )
            }
        }
    }
}
