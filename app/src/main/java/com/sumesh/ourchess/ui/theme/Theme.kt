package com.sumesh.ourchess.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Calm, low-glare palette tuned for older eyes.
object BoardColors {
    val LightSquare = Color(0xFFF0E6CC)
    val DarkSquare = Color(0xFF7A9A6A)
    val LightSquareText = Color(0xFFF0E6CC)
    val DarkSquareText = Color(0xFF4F6B42)
    val LastMove = Color(0xFFF2CF3B)
    val Selected = Color(0xFF2F6FD0)
    val Target = Color(0x99223344)
    val Check = Color(0xFFE53935)
}

private val AppColors = lightColorScheme(
    primary = Color(0xFF2E5E3E),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFE4DCC8),
    onSecondaryContainer = Color(0xFF1F1F1F),
    background = Color(0xFFF7F3EA),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFF7F3EA),
    onSurface = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFFFFFBF2),
    error = Color(0xFFB3261E),
)

// Everything a step larger than Material defaults, for readability.
private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        labelLarge = labelLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun OurChessTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
