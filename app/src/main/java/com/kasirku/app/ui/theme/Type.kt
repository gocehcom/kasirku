package com.kasirku.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Skala tipe Material3 — ukuran tidak dipilih manual per layar.
val KasirKuType = Typography(
    displaySmall = TextStyle(FontFamily.Default, FontWeight.Bold, 36.sp, 44.sp),
    headlineMedium = TextStyle(FontFamily.Default, FontWeight.SemiBold, 28.sp, 36.sp),
    headlineSmall = TextStyle(FontFamily.Default, FontWeight.SemiBold, 24.sp, 32.sp),
    titleLarge = TextStyle(FontFamily.Default, FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium = TextStyle(FontFamily.Default, FontWeight.Medium, 16.sp, 24.sp),
    titleSmall = TextStyle(FontFamily.Default, FontWeight.Medium, 14.sp, 20.sp),
    bodyLarge = TextStyle(FontFamily.Default, FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = TextStyle(FontFamily.Default, FontWeight.Normal, 14.sp, 20.sp),
    bodySmall = TextStyle(FontFamily.Default, FontWeight.Normal, 12.sp, 16.sp),
    labelLarge = TextStyle(FontFamily.Default, FontWeight.Medium, 14.sp, 20.sp),
    labelMedium = TextStyle(FontFamily.Default, FontWeight.Medium, 12.sp, 16.sp),
)
