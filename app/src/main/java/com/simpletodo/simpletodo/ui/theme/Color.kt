package com.simpletodo.simpletodo.ui.theme

import androidx.compose.ui.graphics.Color

// One neutral graphite ramp for every surface, border and text role, plus a
// single accent taken from the launcher icon's violet. The accent is reserved
// for the primary action, the current selection, checked items and
// "important" schedules — everything else stays neutral.

// Light
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightRaised = Color(0xFFF4F4F5)
val LightTextPrimary = Color(0xFF171717)
val LightTextSecondary = Color(0xFF525252)
val LightTextMuted = Color(0xFF737373)
val LightHairline = Color(0x14171717) // primary text at 8%
val LightAccent = Color(0xFF624DE6) // icon violet, 5.6:1 against white
val LightError = Color(0xFFDC2626)

// Dark
val DarkBackground = Color(0xFF141415)
val DarkSurface = Color(0xFF19191B)
val DarkRaised = Color(0xFF232325)
val DarkTextPrimary = Color(0xFFEDEDED)
val DarkTextSecondary = Color(0xFFA1A1A6)
val DarkTextMuted = Color(0xFF85858B)
val DarkHairline = Color(0x14EDEDED)
val DarkAccent = Color(0xFFA996FF) // lighter step of the icon violet, 7.4:1 on the page
val DarkError = Color(0xFFE5484D)
