package com.vectorminds.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// VectorMind Premium Palette
//
// Inspiration: Linear, Raycast, Arc, Vercel, Anthropic, Bloomberg Terminal.
// Goals:
//   • Restrained accents (NEVER full-saturation neon).
//   • Deep neutrals that read like graphite, not blue plastic.
//   • Strong on-color contrast for accessibility.
//   • A SINGLE source of truth → semantic tokens (see [VmColors]).
//
// Existing screens still import the legacy names below by-name; we keep the
// names but tune values so nothing screams. Eventually all screens should
// migrate to MaterialTheme color roles + LocalVmColors instead.
// ─────────────────────────────────────────────────────────────────────────────

// Primary: Electric Indigo (was raw cyan #00E5FF)
val CyanPrimary       = Color(0xFF7EA6FF)   // refined indigo-cyan, calm
val CyanLight         = Color(0xFFB7CDFF)
val CyanDark          = Color(0xFF3F66C9)

// Secondary: Royal Violet (was raw purple)
val PurpleSecondary   = Color(0xFFA98BFF)
val PurpleLight       = Color(0xFFD4C4FF)
val PurpleDark        = Color(0xFF6E4FE6)

// Tertiary: Pro Emerald (toned down)
val EmeraldTertiary   = Color(0xFF44D6A2)
val EmeraldLight      = Color(0xFF95EBC9)
val EmeraldDark       = Color(0xFF1F8F66)

// Surfaces — Graphite/Onyx scale (was a too-blue navy ramp)
val Navy10            = Color(0xFF07080C)   // #BACKGROUND  (AMOLED-true)
val Navy15            = Color(0xFF0B0D14)   // #SURFACE
val Navy20            = Color(0xFF11141E)   // #SURFACE_RAISED
val Navy25            = Color(0xFF161A26)   // #SURFACE_VARIANT (cards)
val Navy30            = Color(0xFF1F2433)   // #OUTLINE / divider
val Navy35            = Color(0xFF2A3145)   // borders on hover

// Neutrals (for light mode)
val Surface80         = Color(0xFF8B92A6)
val Surface90         = Color(0xFFE2E5EE)
val Surface95         = Color(0xFFF1F3F8)
val Surface99         = Color(0xFFFAFBFD)

// Semantic — calmer, less arcade
val ErrorRed          = Color(0xFFEB5E6F)
val WarningAmber      = Color(0xFFE8A341)
val SuccessGreen      = Color(0xFF44D6A2)
val InfoBlue          = Color(0xFF7EA6FF)

// Score colors (for emergence/novelty/impact)
val ScoreHigh         = SuccessGreen
val ScoreMedium       = WarningAmber
val ScoreLow          = ErrorRed
