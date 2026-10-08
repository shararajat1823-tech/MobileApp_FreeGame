package com.miniplay.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale tuned for a premium, mobile-first feel. We ship the platform font
 * family (no bundled/licensed fonts) but control weight, size, tracking and
 * line-height to get a clear hierarchy. Scores and timers use the "number"
 * styles below, which lean heavier and tighter.
 */
private val Default = FontFamily.Default

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val MiniPlayTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.02).em,
        lineHeightStyle = tightLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em,
        lineHeightStyle = tightLineHeight,
    ),
    headlineLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.em,
    ),
    titleSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.01.em,
    ),
    labelMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em,
    ),
    labelSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em,
    ),
)

/** Monospaced-feel numeric styles for scores, timers and tile values. */
object NumberStyles {
    val hero = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Black,
        fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.02).em,
    )
    val large = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.01).em,
    )
    val medium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 24.sp,
    )
}
