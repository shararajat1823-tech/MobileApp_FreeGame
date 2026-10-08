package com.miniplay.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw colour tokens. Nothing in the UI references a hex value directly — screens
 * and components pull colours from [androidx.compose.material3.MaterialTheme] or
 * from [MiniPlayColors] (the extended palette exposed via [LocalMiniPlayColors]).
 *
 * The brand is a warm violet/indigo with a coral + mint accent pairing, chosen to
 * read as modern and playful while keeping AA contrast in both light and dark.
 */
internal object Palette {
    // Brand violet ramp
    val Violet10 = Color(0xFF17123A)
    val Violet20 = Color(0xFF2A1F63)
    val Violet30 = Color(0xFF3F2F8F)
    val Violet40 = Color(0xFF5B2FE0)
    val Violet50 = Color(0xFF6C4CF1)
    val Violet60 = Color(0xFF8A6FF5)
    val Violet80 = Color(0xFFCBBDFF)
    val Violet90 = Color(0xFFE7E0FF)
    val Violet95 = Color(0xFFF4F0FF)

    // Secondary — teal/mint
    val Teal30 = Color(0xFF0C4F4A)
    val Teal40 = Color(0xFF12857B)
    val Teal50 = Color(0xFF17B3A4)
    val Teal80 = Color(0xFF9BEFE4)
    val Teal90 = Color(0xFFCFF8F1)

    // Tertiary — coral
    val Coral30 = Color(0xFF7A1F3A)
    val Coral40 = Color(0xFFC33156)
    val Coral50 = Color(0xFFF2567C)
    val Coral80 = Color(0xFFFFB1C4)
    val Coral90 = Color(0xFFFFD9E1)

    // Neutrals (light)
    val Neutral10 = Color(0xFF131218)
    val Neutral20 = Color(0xFF1E1C25)
    val Neutral22 = Color(0xFF232029)
    val Neutral24 = Color(0xFF2A2733)
    val Neutral90 = Color(0xFFE6E2EC)
    val Neutral93 = Color(0xFFEDEAF3)
    val Neutral95 = Color(0xFFF4F1F9)
    val Neutral98 = Color(0xFFFCFAFF)
    val Neutral99 = Color(0xFFFFFBFF)

    val NeutralVariant30 = Color(0xFF494553)
    val NeutralVariant50 = Color(0xFF79748A)
    val NeutralVariant60 = Color(0xFF938DA6)
    val NeutralVariant80 = Color(0xFFCAC3DC)
    val NeutralVariant90 = Color(0xFFE7DFF4)

    // Semantic
    val Success = Color(0xFF2FBF71)
    val SuccessContainer = Color(0xFFBDF0D4)
    val OnSuccessContainer = Color(0xFF07371F)
    val Warning = Color(0xFFF2A516)
    val WarningContainer = Color(0xFFFFE6B0)
    val OnWarningContainer = Color(0xFF3D2A00)
    val Error40 = Color(0xFFBA1A1A)
    val Error80 = Color(0xFFFFB4AB)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}
