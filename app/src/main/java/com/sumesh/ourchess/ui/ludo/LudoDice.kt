package com.sumesh.ourchess.ui.ludo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A big dice to tap. Its edge takes the colour of the player whose turn it is. */
@Composable
fun LudoDice(face: Int, color: Color, enabled: Boolean, onRoll: () -> Unit, size: Dp = 104.dp, label: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = onRoll,
            enabled = enabled,
            shape = RoundedCornerShape(size * 0.2f),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(size * 0.06f, color),
            shadowElevation = if (enabled) 6.dp else 1.dp,
            modifier = Modifier
                .size(size)
                .alpha(if (enabled) 1f else 0.85f)
                .semantics { contentDescription = if (enabled) "Dice, tap to roll" else "Dice showing $face" },
        ) {
            Canvas(Modifier.size(size)) { drawPips(face) }
        }
        if (label != null) Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

/** A small, non-interactive dice face (used as the Ludo icon in menus). */
@Composable
fun DiceFace(face: Int, size: Dp) {
    Surface(shape = RoundedCornerShape(size * 0.2f), color = Color.White, border = androidx.compose.foundation.BorderStroke(size * 0.06f, Color(0xFF2E5E3E))) {
        Canvas(Modifier.size(size)) { drawPips(face) }
    }
}

private fun DrawScope.drawPips(face: Int) {
    val s = size.minDimension
    val r = s * 0.085f
    val lo = s * 0.27f; val mid = s * 0.5f; val hi = s * 0.73f
    val spots = when (face) {
        1 -> listOf(mid to mid)
        2 -> listOf(lo to lo, hi to hi)
        3 -> listOf(lo to lo, mid to mid, hi to hi)
        4 -> listOf(lo to lo, hi to lo, lo to hi, hi to hi)
        5 -> listOf(lo to lo, hi to lo, mid to mid, lo to hi, hi to hi)
        else -> listOf(lo to lo, hi to lo, lo to mid, hi to mid, lo to hi, hi to hi)
    }
    for ((x, y) in spots) drawCircle(Color(0xFF1A1A1A), r, Offset(x, y))
}
