package com.sumesh.ourchess.ui.checkers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.IntOffset
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.board.BoardCoordinates
import com.sumesh.ourchess.ui.board.COORD_GUTTER
import com.sumesh.ourchess.ui.theme.LocalPlayerSettings
import com.sumesh.ourchess.ui.theme.palette
import kotlin.math.roundToInt

/** A move to slide along its path (hop by hop for multi-captures); [id] changes for every new move. */
data class CheckersAnimation(val id: Int, val path: List<Square>, val durationMs: Int)

/**
 * The checkers board, drawn like the chess board (same colours and coordinate strips) with large discs.
 * Moves are made by tapping a piece, then where it should go — hop by hop for multi-captures.
 */
@Composable
fun CheckersBoard(
    pieces: Map<Square, CheckersPiece>,
    flipped: Boolean,
    selected: Square?,
    /** Squares already visited in a multi-capture being entered. */
    partialPath: List<Square>,
    targets: Set<Square>,
    lastPath: List<Square>?,
    animation: CheckersAnimation?,
    targetsAreHint: Boolean,
    onSquareTap: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap by rememberUpdatedState(onSquareTap)
    val settings = LocalPlayerSettings.current
    val palette = settings.boardTheme.palette()
    val visibleTargets = if (settings.showLegalMoves || targetsAreHint) targets else emptySet()

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val squareSize = minOf(maxWidth, maxHeight) / (8 + COORD_GUTTER)
        val gutter = squareSize * COORD_GUTTER
        val boardSize = squareSize * 8
        val squarePx = with(LocalDensity.current) { squareSize.toPx() }

        fun topLeft(sq: Square): Offset {
            val col = if (flipped) 7 - sq.file else sq.file
            val row = if (flipped) sq.rank else 7 - sq.rank
            return Offset(col * squarePx, row * squarePx)
        }

        fun center(sq: Square) = topLeft(sq) + Offset(squarePx / 2, squarePx / 2)

        fun squareAt(pos: Offset): Square? {
            val col = (pos.x / squarePx).toInt()
            val row = (pos.y / squarePx).toInt()
            if (pos.x < 0 || pos.y < 0 || col !in 0..7 || row !in 0..7) return null
            return if (flipped) Square(7 - col, row) else Square(col, 7 - row)
        }

        val progress = remember { Animatable(1f) }
        var animatedId by remember { mutableIntStateOf(-1) }
        LaunchedEffect(animation?.id) {
            val anim = animation ?: return@LaunchedEffect
            progress.snapTo(0f)
            animatedId = anim.id
            progress.animateTo(1f, tween(anim.durationMs, easing = LinearEasing))
        }
        val animProgress = when {
            animation == null -> 1f
            animatedId != animation.id -> 0f
            else -> progress.value
        }

        /** Where the moving piece is drawn part-way along its (possibly multi-hop) path. */
        fun animatedCenter(path: List<Square>, t: Float): Offset {
            val hops = path.size - 1
            val x = (t * hops).coerceIn(0f, hops.toFloat())
            val i = x.toInt().coerceAtMost(hops - 1)
            val a = center(path[i])
            val b = center(path[i + 1])
            return a + (b - a) * (x - i)
        }

        Box(Modifier.size(boardSize + gutter)) {
            BoardCoordinates(flipped, squareSize, gutter)
            Box(
                Modifier
                    .offset(x = gutter)
                    .size(boardSize)
                    .pointerInput(flipped, squarePx) { detectTapGestures { pos -> squareAt(pos)?.let(tap) } },
            ) {
                Canvas(Modifier.size(boardSize)) {
                    val sqSize = Size(squarePx, squarePx)
                    for (sq in Square.all) {
                        val tl = topLeft(sq)
                        // a1 is dark in checkers.
                        drawRect(if ((sq.file + sq.rank) % 2 == 0) palette.darkSquare else palette.lightSquare, tl, sqSize)
                        if (lastPath != null && sq in lastPath) drawRect(palette.lastMove.copy(alpha = 0.7f), tl, sqSize)
                        if (sq == selected || sq in partialPath) {
                            drawRect(palette.selected.copy(alpha = 0.45f), tl, sqSize)
                            val w = squarePx * 0.05f
                            drawRect(palette.selected, tl + Offset(w / 2, w / 2), Size(squarePx - w, squarePx - w), style = Stroke(w))
                        }
                    }
                    for (sq in visibleTargets) {
                        drawCircle(palette.target, radius = squarePx * 0.18f, center = center(sq))
                    }
                    val moving = animation?.takeIf { animProgress < 1f }?.path?.last()
                    for ((sq, piece) in pieces) {
                        val c = if (sq == moving) animatedCenter(animation!!.path, animProgress) else center(sq)
                        drawCheckersPiece(c, squarePx * 0.4f, piece)
                    }
                }
                // TalkBack: one node per square.
                for (sq in Square.all) {
                    val tl = topLeft(sq)
                    val piece = pieces[sq]
                    val description = buildString {
                        append(sq.name).append(", ")
                        append(piece?.let { "${it.color.name.lowercase()} ${if (it.king) "king" else "piece"}" } ?: "empty")
                        if (sq == selected) append(", selected")
                        if (sq in visibleTargets) append(", you can move here")
                    }
                    Box(
                        Modifier
                            .offset { IntOffset(tl.x.roundToInt(), tl.y.roundToInt()) }
                            .size(squareSize)
                            .clearAndSetSemantics {
                                contentDescription = description
                                onClick { tap(sq); true }
                            },
                    )
                }
            }
        }
    }
}

private val WhiteFill = Color(0xFFF6F1E4)
private val WhiteRim = Color(0xFFB9AE90)
private val BlackFill = Color(0xFF262626)
private val BlackRim = Color(0xFF6E6E6E)
private val Outline = Color(0xFF151515)
private val Gold = Color(0xFFE2B42A)

/** A large disc with a clear outline; kings carry a gold crown. */
fun DrawScope.drawCheckersPiece(center: Offset, radius: Float, piece: CheckersPiece) {
    val white = piece.color == CheckersColor.WHITE
    // Soft shadow so pieces stand out from the square.
    drawCircle(Color(0x55000000), radius, center + Offset(radius * 0.06f, radius * 0.1f))
    drawCircle(
        Brush.radialGradient(
            if (white) listOf(Color.White, WhiteFill) else listOf(Color(0xFF4A4A4A), BlackFill),
            center = center - Offset(radius * 0.3f, radius * 0.3f),
            radius = radius * 1.4f,
        ),
        radius,
        center,
    )
    drawCircle(if (white) Outline else Color(0xFFDADADA), radius, center, style = Stroke(radius * 0.12f))
    drawCircle(if (white) WhiteRim else BlackRim, radius * 0.66f, center, style = Stroke(radius * 0.1f))
    if (piece.king) drawCrown(center, radius * 0.55f)
}

private fun DrawScope.drawCrown(center: Offset, size: Float) {
    val w = size * 1.5f
    val h = size * 1.05f
    val left = center.x - w / 2
    val bottom = center.y + h * 0.45f
    val top = center.y - h * 0.55f
    val crown = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + h * 0.25f)
        lineTo(left + w * 0.25f, top + h * 0.55f)
        lineTo(left + w * 0.5f, top)
        lineTo(left + w * 0.75f, top + h * 0.55f)
        lineTo(left + w, top + h * 0.25f)
        lineTo(left + w, bottom)
        close()
    }
    drawPath(crown, Gold)
    drawPath(crown, Outline, style = Stroke(size * 0.12f))
}
