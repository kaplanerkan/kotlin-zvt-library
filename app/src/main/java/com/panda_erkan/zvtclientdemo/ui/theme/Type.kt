package com.panda_erkan.zvtclientdemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.panda_erkan.zvtclientdemo.R

val Montserrat = FontFamily(
    Font(R.font.montserrat_medium, FontWeight.Medium)
)

val ZvtTypography = Typography(
    displayLarge = TextStyle(fontFamily = Montserrat, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = Montserrat, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = Montserrat, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = Montserrat, fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = Montserrat, fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = Montserrat, fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = Montserrat, fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Montserrat, fontSize = 15.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontFamily = Montserrat, fontSize = 13.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = Montserrat, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = Montserrat, fontSize = 12.sp),
    bodySmall = TextStyle(fontFamily = Montserrat, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = Montserrat, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = Montserrat, fontSize = 11.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Montserrat, fontSize = 10.sp, fontWeight = FontWeight.Medium),
)
