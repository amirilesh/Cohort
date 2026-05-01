package com.cohort.ui.theme

import androidx.compose.ui.graphics.Color

// ── Iridescent accent palette ────────────────────────────────────────────────
val IrisIndigo  = Color(0xFF818CF8)   // indigo-400  — primary iris
val IrisViolet  = Color(0xFFA78BFA)   // violet-400  — secondary iris
val IrisCyan    = Color(0xFF67E8F9)   // cyan-300    — logo teal / iris cyan
val IrisEmerald = Color(0xFF34D399)   // emerald-400 — optional iris

// ── Dark backgrounds — refined 3-layer depth system ──────────────────────────
// The key to premium dark UI: clearly distinct surface layers
val NavyDeep    = Color(0xFF0B0E1A)   // deepest background — almost black with blue tint
val NavySurface = Color(0xFF131726)   // elevated surface — cards layer 1
val NavyCard    = Color(0xFF191D2E)   // card/container — slightly lighter than surface
val NavyElevated = Color(0xFF1F2438)  // interactive/highlighted cards
val BorderDark  = Color(0xFF252A3E)   // subtle border — barely visible separation

// ── Logo gradient ─────────────────────────────────────────────────────────────
val LogoSilver  = Color(0xFFE0E7FF)   // indigo-100 (silver sheen on C arc)
val LogoCyan    = Color(0xFF67E8F9)   // cyan-300   (atom + C ends)
val LogoNavy    = Color(0xFF1E1B4B)   // deep navy  (C arc bottom)

// ── Dark text — better contrast hierarchy ─────────────────────────────────────
val TextPrimary   = Color(0xFFECEFF4)   // near-white — primary text
val TextSecondary = Color(0xFF9BA3B8)   // muted slate — secondary / descriptions
val TextTertiary  = Color(0xFF6B7494)   // dimmer — metadata, timestamps

// ── Primary brand (kept for Material3 slots) ──────────────────────────────────
val Indigo600   = Color(0xFF6366F1)   // primary on dark
val Indigo700   = Color(0xFF4F46E5)
val Indigo100   = Color(0xFF1A1F42)   // primaryContainer (dark) — deeper, less saturated
val Indigo50    = Color(0xFF161B3A)
val Violet600   = Color(0xFF8B5CF6)
val Cyan500     = Color(0xFF06B6D4)

// ── Light theme palette (kept for light mode support) ────────────────────────
val BackgroundLight     = Color(0xFFF8FAFC)
val SurfaceVariantLight = Color(0xFFEEF2FF)
val NeutralGrey         = Color(0xFF64748B)
val OnBackground        = Color(0xFF0F172A)
val SlateLight          = Color(0xFFE2E8F0)

// ── Dark base (compatibility) ────────────────────────────────────────────────
val Indigo300   = Color(0xFFA5B4FC)
val Indigo200   = Color(0xFFC7D2FE)
val Indigo900   = Color(0xFF312E81)