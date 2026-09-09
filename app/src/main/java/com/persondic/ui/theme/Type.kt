package com.persondic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

private val Default = Typography()

/**
 * Material 3's default letter spacing is tuned for Latin text. Hangul is already evenly spaced by
 * its own shape, so the extra tracking just makes it look loose. Everything else is left alone.
 */
val Typography = Typography(
    displayLarge = Default.displayLarge.copy(letterSpacing = 0.sp),
    displayMedium = Default.displayMedium.copy(letterSpacing = 0.sp),
    displaySmall = Default.displaySmall.copy(letterSpacing = 0.sp),
    headlineLarge = Default.headlineLarge.copy(letterSpacing = 0.sp),
    headlineMedium = Default.headlineMedium.copy(letterSpacing = 0.sp),
    headlineSmall = Default.headlineSmall.copy(letterSpacing = 0.sp),
    titleLarge = Default.titleLarge.copy(letterSpacing = 0.sp),
    titleMedium = Default.titleMedium.copy(letterSpacing = 0.sp),
    titleSmall = Default.titleSmall.copy(letterSpacing = 0.sp),
    bodyLarge = Default.bodyLarge.copy(letterSpacing = 0.sp),
    bodyMedium = Default.bodyMedium.copy(letterSpacing = 0.sp),
    bodySmall = Default.bodySmall.copy(letterSpacing = 0.sp),
    labelLarge = Default.labelLarge.copy(letterSpacing = 0.sp),
    labelMedium = Default.labelMedium.copy(letterSpacing = 0.sp),
    labelSmall = Default.labelSmall.copy(letterSpacing = 0.sp),
)
