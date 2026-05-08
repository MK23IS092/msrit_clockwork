package com.vectorminds.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// SEMANTIC TOKENS (Vm = VectorMind)
//
// Material's color roles are not enough for a research-grade UI: we need
// "muted text", "score bands", "surface raised vs sunken", elevation tints,
// status colors, etc. These tokens cover that gap.
//
// Usage:  val vm = LocalVmColors.current ; Text(color = vm.textMuted)
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class VmColors(
    // Surface ladder
    val background: Color,
    val surface: Color,           // top-level cards
    val surfaceElevated: Color,   // raised tile (hover/active)
    val surfaceSunken: Color,     // inset / inputs
    val border: Color,
    val borderStrong: Color,

    // Text
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,

    // Brand & accents
    val brand: Color,             // primary action
    val brandSoft: Color,         // bg of branded chips
    val brandOnSoft: Color,       // text on brandSoft
    val violet: Color,
    val emerald: Color,
    val amber: Color,
    val rose: Color,

    // Status (semantic, NOT decorative)
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,

    // Score bands (for emergence/novelty/impact)
    val scoreHigh: Color,
    val scoreMid: Color,
    val scoreLow: Color,

    // Special — used for subtle decorative overlays only
    val glow: Color,
)

@Immutable
data class VmShape(
    val xs: Dp = 6.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
)

@Immutable
data class VmSpacing(
    val xxs: Dp = 2.dp,
    val xs:  Dp = 4.dp,
    val sm:  Dp = 8.dp,
    val md:  Dp = 12.dp,
    val lg:  Dp = 16.dp,
    val xl:  Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

@Immutable
data class VmElevation(
    val card: Dp = 0.dp,          // we draw cards with borders, not shadows
    val raised: Dp = 2.dp,
    val sheet: Dp = 12.dp,
    val dock: Dp = 16.dp,
)

// ─── Default token values ────────────────────────────────────────────────────

val DarkVmColors = VmColors(
    background       = Navy10,
    surface          = Navy15,
    surfaceElevated  = Navy20,
    surfaceSunken    = Color(0xFF050608),
    border           = Navy30,
    borderStrong     = Navy35,

    text             = Color(0xFFE8EBF2),
    textMuted        = Color(0xFF98A0B3),
    textFaint        = Color(0xFF5C6479),

    brand            = CyanPrimary,
    brandSoft        = Color(0x227EA6FF),     // 13% indigo
    brandOnSoft      = CyanLight,
    violet           = PurpleSecondary,
    emerald          = EmeraldTertiary,
    amber            = WarningAmber,
    rose             = ErrorRed,

    success          = SuccessGreen,
    warning          = WarningAmber,
    danger           = ErrorRed,
    info             = InfoBlue,

    scoreHigh        = ScoreHigh,
    scoreMid         = ScoreMedium,
    scoreLow         = ScoreLow,

    glow             = Color(0x227EA6FF),
)

val LightVmColors = VmColors(
    background       = Surface99,
    surface          = Color(0xFFFFFFFF),
    surfaceElevated  = Color(0xFFFFFFFF),
    surfaceSunken    = Surface95,
    border           = Color(0xFFE3E6EF),
    borderStrong     = Color(0xFFCED2DE),

    text             = Color(0xFF101422),
    textMuted        = Color(0xFF4F576A),
    textFaint        = Color(0xFF8B92A6),

    brand            = CyanDark,
    brandSoft        = Color(0x223F66C9),
    brandOnSoft      = CyanDark,
    violet           = PurpleDark,
    emerald          = EmeraldDark,
    amber            = Color(0xFFB07A1F),
    rose             = Color(0xFFC04050),

    success          = EmeraldDark,
    warning          = Color(0xFFB07A1F),
    danger           = Color(0xFFC04050),
    info             = CyanDark,

    scoreHigh        = EmeraldDark,
    scoreMid         = Color(0xFFB07A1F),
    scoreLow         = Color(0xFFC04050),

    glow             = Color(0x113F66C9),
)

val LocalVmColors    = staticCompositionLocalOf<VmColors>    { DarkVmColors }
val LocalVmShape     = staticCompositionLocalOf { VmShape() }
val LocalVmSpacing   = staticCompositionLocalOf { VmSpacing() }
val LocalVmElevation = staticCompositionLocalOf { VmElevation() }

object Vm {
    val colors: VmColors
        @Composable @ReadOnlyComposable
        get() = LocalVmColors.current
    val shape: VmShape
        @Composable @ReadOnlyComposable
        get() = LocalVmShape.current
    val spacing: VmSpacing
        @Composable @ReadOnlyComposable
        get() = LocalVmSpacing.current
    val elevation: VmElevation
        @Composable @ReadOnlyComposable
        get() = LocalVmElevation.current
}

// ─── Motion tokens ───────────────────────────────────────────────────────────

object VmMotion {
    const val FAST = 150
    const val MEDIUM = 240
    const val SLOW = 360
    const val EMPHASIS = 480
}
