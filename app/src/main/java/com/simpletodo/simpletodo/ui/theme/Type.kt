package com.simpletodo.simpletodo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A short scale with two weights (400 and 500). Hierarchy comes from size and
// color, not weight. Letter spacing is 0 everywhere: Material's default
// tracking spreads Hangul out noticeably.
private val Regular = FontWeight.Normal
private val Medium = FontWeight.Medium

val Typography = Typography(
    // The date at the top of the list — the one large element on screen.
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = Medium, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = Medium, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = Medium, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = Medium, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = Medium, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = Regular, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = Regular, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = Regular, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = Medium, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = Medium, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = Medium, letterSpacing = 0.sp),
)
