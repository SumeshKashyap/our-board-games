package com.sumesh.ourchess.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sumesh.ourchess.game.Move
import com.sumesh.ourchess.game.Piece
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.theme.BoardColors
import kotlin.math.roundToInt

/** A move to slide across the board; [id] changes for every new move so repeats still animate. */
data class MoveAnimation(val id: Int, val from: Square, val to: Square, val durationMs: Int)

/**
 * The chess board. Fills the largest square that fits.
 * Pieces can be moved by tapping (piece, then destination) or by dragging.
 *
 * @param flipped true to show the board from Black's side.
 * @param onDragStart called when a drag begins on a square; return true to allow dragging that piece.
 */
@Composable
fun ChessBoard(
    pieces: Map<Square, Piece>,
    flipped: Boolean,
    selected: Square?,
    targets: Set<Square>,
    lastMove: Move?,
    checkSquare: Square?,
    animation: MoveAnimation?,
    onSquareTap: (Square) -> Unit,
    onDragStart: (Square) -> Boolean,
    onDrop: (from: Square, to: Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap by rememberUpdatedState(onSquareTap)
    val dragStart by rememberUpdatedState(onDragStart)
    val drop by rememberUpdatedState(onDrop)

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight)
        val squareSize = boardSize / 8
        val density = LocalDensity.current
        val squarePx = with(density) { squareSize.toPx() }

        // Screen position (top-left, in px) of a square, honouring board orientation.
        fun topLeft(sq: Square): Offset {
            val col = if (flipped) 7 - sq.file else sq.file
            val row = if (flipped) sq.rank else 7 - sq.rank
            return Offset(col * squarePx, row * squarePx)
        }

        fun squareAt(pos: Offset): Square? {
            val col = (pos.x / squarePx).toInt()
            val row = (pos.y / squarePx).toInt()
            if (pos.x < 0 || pos.y < 0 || col !in 0..7 || row !in 0..7) return null
            return if (flipped) Square(7 - col, row) else Square(col, 7 - row)
        }

        // Slide animation for the last move. Until the effect starts, treat a new move as not yet moved.
        val progress = remember { Animatable(1f) }
        var animatedId by remember { mutableIntStateOf(-1) }
        LaunchedEffect(animation?.id) {
            val anim = animation ?: return@LaunchedEffect
            progress.snapTo(0f)
            animatedId = anim.id
            progress.animateTo(1f, tween(anim.durationMs, easing = FastOutSlowInEasing))
        }
        val animProgress = when {
            animation == null -> 1f
            animatedId != animation.id -> 0f
            else -> progress.value
        }

        var dragFrom by remember { mutableStateOf<Square?>(null) }
        var dragPos by remember { mutableStateOf(Offset.Zero) }

        val textMeasurer = rememberTextMeasurer()
        val coordStyle = TextStyle(
            fontSize = with(density) { (squareSize * 0.22f).toSp() },
            fontWeight = FontWeight.Bold,
        )

        Box(
            Modifier
                .size(boardSize)
                .pointerInput(flipped, squarePx) {
                    detectTapGestures { pos -> squareAt(pos)?.let(tap) }
                }
                .pointerInput(flipped, squarePx) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val sq = squareAt(pos)
                            if (sq != null && dragStart(sq)) {
                                dragFrom = sq
                                dragPos = pos
                            }
                        },
                        onDrag = { change, delta ->
                            if (dragFrom != null) {
                                change.consume()
                                dragPos += delta
                            }
                        },
                        onDragEnd = {
                            val from = dragFrom
                            val to = squareAt(dragPos)
                            dragFrom = null
                            if (from != null && to != null && to != from) drop(from, to)
                        },
                        onDragCancel = { dragFrom = null },
                    )
                },
        ) {
            Canvas(Modifier.size(boardSize)) {
                val sqSize = Size(squarePx, squarePx)
                for (sq in Square.all) {
                    val tl = topLeft(sq)
                    drawRect(if (sq.isLight) BoardColors.LightSquare else BoardColors.DarkSquare, tl, sqSize)
                    if (lastMove != null && (sq == lastMove.from || sq == lastMove.to)) {
                        drawRect(BoardColors.LastMove.copy(alpha = if (sq.isLight) 0.6f else 0.75f), tl, sqSize)
                    }
                    if (sq == selected) {
                        drawRect(BoardColors.Selected.copy(alpha = 0.45f), tl, sqSize)
                        val inset = squarePx * 0.04f
                        drawRect(
                            BoardColors.Selected,
                            tl + Offset(inset / 2, inset / 2),
                            Size(squarePx - inset, squarePx - inset),
                            style = Stroke(inset),
                        )
                    }
                    if (sq == checkSquare) {
                        drawRect(
                            Brush.radialGradient(
                                listOf(BoardColors.Check, BoardColors.Check.copy(alpha = 0f)),
                                center = tl + Offset(squarePx / 2, squarePx / 2),
                                radius = squarePx * 0.7f,
                            ),
                            tl, sqSize,
                        )
                    }
                }
                drawCoordinates(flipped, squarePx, textMeasurer, coordStyle)
                for (sq in targets) {
                    val center = topLeft(sq) + Offset(squarePx / 2, squarePx / 2)
                    if (pieces.containsKey(sq)) {
                        // Capture: a thick ring around the piece.
                        val w = squarePx * 0.09f
                        drawCircle(BoardColors.Target, radius = squarePx / 2 - w / 2, center = center, style = Stroke(w))
                    } else {
                        drawCircle(BoardColors.Target, radius = squarePx * 0.17f, center = center)
                    }
                }
            }

            for ((sq, piece) in pieces) {
                if (sq == dragFrom) continue
                var pos = topLeft(sq)
                if (animation != null && sq == animation.to && animProgress < 1f) {
                    val from = topLeft(animation.from)
                    pos = from + (pos - from) * animProgress
                }
                Image(
                    painter = painterResource(pieceDrawable(piece)),
                    contentDescription = null,
                    modifier = Modifier
                        .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                        .size(squareSize)
                        .zIndex(if (sq == animation?.to) 1f else 0f),
                )
            }

            // The dragged piece follows the finger, slightly enlarged and lifted so it isn't hidden by it.
            dragFrom?.let { from ->
                val piece = pieces[from] ?: return@let
                val scale = 1.3f
                val half = squarePx * scale / 2
                Image(
                    painter = painterResource(pieceDrawable(piece)),
                    contentDescription = null,
                    modifier = Modifier
                        .offset {
                            IntOffset((dragPos.x - half).roundToInt(), (dragPos.y - half * 1.6f).roundToInt())
                        }
                        .size(squareSize * scale)
                        .zIndex(2f),
                )
            }
        }
    }
}

/** File letters along the bottom edge and rank numbers along the left edge, inside the squares. */
private fun DrawScope.drawCoordinates(
    flipped: Boolean,
    squarePx: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle,
) {
    val pad = squarePx * 0.05f
    for (i in 0..7) {
        // Bottom row: files
        val file = if (flipped) 7 - i else i
        val bottomSq = Square(file, if (flipped) 7 else 0)
        val fileText = textMeasurer.measure(('a' + file).toString(), style.copy(color = coordColor(bottomSq)))
        drawText(
            fileText,
            topLeft = Offset(
                (i + 1) * squarePx - fileText.size.width - pad,
                8 * squarePx - fileText.size.height - pad * 0.3f,
            ),
        )
        // Left column: ranks
        val rank = if (flipped) i else 7 - i
        val leftSq = Square(if (flipped) 7 else 0, rank)
        val rankText = textMeasurer.measure((rank + 1).toString(), style.copy(color = coordColor(leftSq)))
        drawText(rankText, topLeft = Offset(pad, i * squarePx + pad * 0.3f))
    }
}

private fun coordColor(sq: Square): Color = if (sq.isLight) BoardColors.DarkSquareText else BoardColors.LightSquareText
