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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sumesh.ourchess.game.Move
import com.sumesh.ourchess.game.Piece
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.game.spoken
import com.sumesh.ourchess.ui.theme.LocalPlayerSettings
import com.sumesh.ourchess.ui.theme.palette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import kotlin.math.roundToInt

/** A move to slide across the board; [id] changes for every new move so repeats still animate. */
data class MoveAnimation(val id: Int, val from: Square, val to: Square, val durationMs: Int)

/**
 * The chess board. Fills the largest square that fits.
 * Pieces can be moved by tapping (piece, then destination) or by dragging.
 *
 * Colours, large-print letters and whether possible moves are shown follow the current player's settings.
 *
 * @param flipped true to show the board from Black's side.
 * @param targetsAreHint true when [targets] is a suggested move (always shown, even if possible moves are hidden).
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
    targetsAreHint: Boolean = false,
    onSquareTap: (Square) -> Unit,
    onDragStart: (Square) -> Boolean,
    onDrop: (from: Square, to: Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap by rememberUpdatedState(onSquareTap)
    val dragStart by rememberUpdatedState(onDragStart)
    val drop by rememberUpdatedState(onDrop)
    val settings = LocalPlayerSettings.current
    val palette = settings.boardTheme.palette()
    val visibleTargets = if (settings.showLegalMoves || targetsAreHint) targets else emptySet()

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Rank numbers sit in a strip to the left and file letters in a strip below, outside the squares.
        val squareSize = minOf(maxWidth, maxHeight) / (8 + COORD_GUTTER)
        val gutter = squareSize * COORD_GUTTER
        val boardSize = squareSize * 8
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

        Box(Modifier.size(boardSize + gutter)) {
            BoardCoordinates(flipped, squareSize, gutter)
            Box(
                Modifier
                    .offset(x = gutter)
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
                        drawRect(if (sq.isLight) palette.lightSquare else palette.darkSquare, tl, sqSize)
                        if (lastMove != null && (sq == lastMove.from || sq == lastMove.to)) {
                            drawRect(palette.lastMove.copy(alpha = if (sq.isLight) 0.6f else 0.75f), tl, sqSize)
                        }
                        if (sq == selected) {
                            drawRect(palette.selected.copy(alpha = 0.45f), tl, sqSize)
                            val inset = squarePx * 0.05f
                            drawRect(
                                palette.selected,
                                tl + Offset(inset / 2, inset / 2),
                                Size(squarePx - inset, squarePx - inset),
                                style = Stroke(inset),
                            )
                        }
                        if (sq == checkSquare) {
                            drawRect(
                                Brush.radialGradient(
                                    listOf(palette.check, palette.check.copy(alpha = 0f)),
                                    center = tl + Offset(squarePx / 2, squarePx / 2),
                                    radius = squarePx * 0.7f,
                                ),
                                tl, sqSize,
                            )
                        }
                    }
                    for (sq in visibleTargets) {
                        val center = topLeft(sq) + Offset(squarePx / 2, squarePx / 2)
                        if (pieces.containsKey(sq)) {
                            // Capture: a thick ring around the piece.
                            val w = squarePx * 0.09f
                            drawCircle(palette.target, radius = squarePx / 2 - w / 2, center = center, style = Stroke(w))
                        } else {
                            drawCircle(palette.target, radius = squarePx * 0.17f, center = center)
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
                    PieceView(
                        piece,
                        settings.largePrint,
                        Modifier
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .size(squareSize)
                            .zIndex(if (sq == animation?.to) 1f else 0f),
                    )
                }

                // Invisible per-square nodes so TalkBack can read and operate the board.
                for (sq in Square.all) {
                    val tl = topLeft(sq)
                    val piece = pieces[sq]
                    val description = buildString {
                        append(sq.name)
                        append(", ")
                        append(piece?.spoken() ?: "empty")
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

                // The dragged piece follows the finger, slightly enlarged and lifted so it isn't hidden by it.
                dragFrom?.let { from ->
                    val piece = pieces[from] ?: return@let
                    val scale = 1.3f
                    val half = squarePx * scale / 2
                    PieceView(
                        piece,
                        settings.largePrint,
                        Modifier
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
}

/** Rank numbers down the left and file letters along the bottom, outside the board, honouring orientation. */
@Composable
internal fun BoardCoordinates(flipped: Boolean, squareSize: Dp, gutter: Dp) {
    // Sized from the square (not the system font size) so the labels always fit their strip.
    val style = TextStyle(
        fontSize = with(LocalDensity.current) { (squareSize * 0.32f).toSp() },
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4A4A4A),
    )
    for (i in 0..7) {
        val rank = if (flipped) i + 1 else 8 - i
        Box(Modifier.offset(y = squareSize * i).size(gutter, squareSize), contentAlignment = Alignment.Center) {
            Text(rank.toString(), style = style)
        }
        val file = 'a' + (if (flipped) 7 - i else i)
        Box(
            Modifier.offset(x = gutter + squareSize * i, y = squareSize * 8).size(squareSize, gutter),
            contentAlignment = Alignment.Center,
        ) {
            Text(file.toString(), style = style)
        }
    }
}

internal const val COORD_GUTTER = 0.45f

/**
 * One piece. In large print, a letter badge (K, Q, R, B, N) in the corner names it.
 * Pawns get no letter: their shape is already distinct and it keeps the board uncluttered.
 */
@Composable
fun PieceView(piece: Piece, largePrint: Boolean, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        Image(painterResource(pieceDrawable(piece)), contentDescription = null, modifier = Modifier.fillMaxSize())
        val letter = piece.type.letter
        if (largePrint && letter != null) {
            val badge = maxWidth * 0.42f
            val white = piece.side == Side.WHITE
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(badge)
                    .background(if (white) Color.White else Color(0xFF1A1A1A), CircleShape)
                    .border(maxWidth * 0.03f, if (white) Color(0xFF1A1A1A) else Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    color = if (white) Color(0xFF1A1A1A) else Color.White,
                    // Sized to the square, not the system font size, so it always fits.
                    fontSize = with(LocalDensity.current) { (badge * 0.62f).toSp() },
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private val PieceType.letter: String?
    get() = when (this) {
        PieceType.KING -> "K"
        PieceType.QUEEN -> "Q"
        PieceType.ROOK -> "R"
        PieceType.BISHOP -> "B"
        PieceType.KNIGHT -> "N"
        PieceType.PAWN -> null
    }
