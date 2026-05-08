package com.vectorminds.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vectorminds.app.ui.theme.Vm

/**
 * Consistent screen header shared by every redesigned tab.
 *
 *   ┌─────────────────────────────┐
 *   │  EYEBROW                    │   ← uppercase 10sp, brand
 *   │  Title                  ⏺   │   ← 28sp Bold + status pill (optional)
 *   │  subtitle                   │   ← muted body
 *   └─────────────────────────────┘
 */
@Composable
fun VmScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val vm = Vm.colors
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(vm.brand),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = vm.brand,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = vm.text,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) trailing()
        }
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = vm.textMuted,
            )
        }
    }
}

/**
 * Tiny live/offline pill: dot + label. Used in screen headers to show
 * backend reachability without hijacking the title bar.
 */
@Composable
fun VmStatusPill(
    label: String,
    healthy: Boolean,
    modifier: Modifier = Modifier,
) {
    val vm = Vm.colors
    val dot = if (healthy) vm.success else vm.danger
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(vm.surfaceElevated)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(dot),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = vm.textMuted,
        )
    }
}

/**
 * Standard background + status-bar padding wrapper for every redesigned tab.
 * Extra bottom padding leaves room for the floating dock.
 */
@Composable
fun VmScreenSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    // The floating dock takes ~56dp + nav-bar inset + 28dp vertical margin,
    // so 96dp wasn't enough on phones with gesture nav. 128dp leaves room
    // for the dock and a clean fade-out below the last card.
    bottomPadding: androidx.compose.ui.unit.Dp = 128.dp,
    content: @Composable () -> Unit,
) {
    val vm = Vm.colors
    Box(
        modifier
            .fillMaxSize()
            .background(vm.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(contentPadding)
                .padding(bottom = bottomPadding),
        ) {
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
