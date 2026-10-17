package com.sumesh.ourchess.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.sumesh.ourchess.data.BoardTheme
import com.sumesh.ourchess.data.PlayerSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colours for one board theme. Text colours are for the coordinates drawn on each square colour. */
data class BoardPalette(
    val lightSquare: Color,
    val darkSquare: Color,
    val textOnLight: Color,
    val textOnDark: Color,
    val lastMove: Color,
    val selected: Color,
    val target: Color,
    val check: Color = Color(0xFFE53935),
)

fun BoardTheme.palette(): BoardPalette = when (this) {
    // Calm, low-glare default tuned for older eyes.
    BoardTheme.GREEN -> BoardPalette(
        lightSquare = Color(0xFFF0E6CC), darkSquare = Color(0xFF7A9A6A),
        textOnLight = Color(0xFF4F6B42), textOnDark = Color(0xFFF0E6CC),
        lastMove = Color(0xFFF2CF3B), selected = Color(0xFF2F6FD0), target = Color(0x99223344),
    )
    BoardTheme.WOOD -> BoardPalette(
        lightSquare = Color(0xFFF0D9B5), darkSquare = Color(0xFFB58863),
        textOnLight = Color(0xFF8B6340), textOnDark = Color(0xFFF0D9B5),
        lastMove = Color(0xFFF5D547), selected = Color(0xFF2F6FD0), target = Color(0x99302010),
    )
    BoardTheme.BLUE -> BoardPalette(
        lightSquare = Color(0xFFDEE3E6), darkSquare = Color(0xFF8CA2AD),
        textOnLight = Color(0xFF5D7380), textOnDark = Color(0xFFDEE3E6),
        lastMove = Color(0xFFF2CF3B), selected = Color(0xFF1B5E20), target = Color(0x99102030),
    )
    // Strong yellow/blue contrast for weaker eyesight; black pieces stay clear on the blue.
    BoardTheme.HIGH_CONTRAST -> BoardPalette(
        lightSquare = Color(0xFFFFF4A8), darkSquare = Color(0xFF4A7BC8),
        textOnLight = Color(0xFF1A1A1A), textOnDark = Color(0xFFFFFFFF),
        lastMove = Color(0xFFFF8A00), selected = Color(0xFFD50000), target = Color(0xCC000000),
    )
}

/** The current player's settings, available to every screen (defaults before a player is chosen). */
val LocalPlayerSettings = staticCompositionLocalOf { PlayerSettings() }

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
