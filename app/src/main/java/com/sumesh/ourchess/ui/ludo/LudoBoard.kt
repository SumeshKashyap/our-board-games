package com.sumesh.ourchess.ui.ludo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.sumesh.ourchess.ludo.Cell
import com.sumesh.ourchess.ludo.LudoColor
import com.sumesh.ourchess.ludo.LudoGeometry
import com.sumesh.ourchess.ludo.LudoState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Main and light shades for each colour, strong enough to tell apart for older eyes. */
fun LudoColor.main(): Color = when (this) {
    LudoColor.RED -> Color(0xFFD63B32)
    LudoColor.GREEN -> Color(0xFF2E9447)
    LudoColor.YELLOW -> Color(0xFFE9B319)
    LudoColor.BLUE -> Color(0xFF2F6CC0)
}

fun LudoColor.light(): Color = when (this) {
    LudoColor.RED -> Color(0xFFF7CFCB)
    LudoColor.GREEN -> Color(0xFFC9EAD1)
    LudoColor.YELLOW -> Color(0xFFFAEAB5)
    LudoColor.BLUE -> Color(0xFFC9DAF3)
}

/** A token travelling from progress [from] to [to], hopping square by square. */
data class LudoAnimation(val id: Int, val color: LudoColor, val token: Int, val from: Int, val to: Int)

private const val HOP_MS = 140

/**
 * The Ludo board on a 15×15 grid. Tokens of the current player that can move pulse; tapping one moves it.
 */
@Composable
fun LudoBoard(
    state: LudoState,
    movable: Set<Int>,
    animation: LudoAnimation?,
    onTokenTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap by rememberUpdatedState(onTokenTap)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight)
        val cellPx = with(LocalDensity.current) { boardSize.toPx() } / 15f

        // Hop-by-hop movement.
        val progress = remember { Animatable(1f) }
        var animatedId by remember { mutableIntStateOf(-1) }
        LaunchedEffect(animation?.id) {
            val anim = animation ?: return@LaunchedEffect
            progress.snapTo(0f)
            animatedId = anim.id
            val hops = if (anim.from == LudoGeometry.YARD) 1 else anim.to - anim.from
            progress.animateTo(1f, tween(HOP_MS * hops.coerceAtLeast(1), easing = LinearEasing))
        }
        val animProgress = when {
            animation == null -> 1f
            animatedId != animation.id -> 0f
            else -> progress.value
        }

        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            0.6f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse",
        )

        val layout = tokenCenters(state, cellPx, animation, animProgress)

        Box(
            Modifier
                .size(boardSize)
                .pointerInput(state, movable, cellPx) {
                    detectTapGestures { pos ->
                        // The nearest movable token within about one square of the tap.
                        layout.filter { it.color == state.currentColor && it.token in movable }
                            .minByOrNull { hypot(it.center.x - pos.x, it.center.y - pos.y) }
                            ?.takeIf { hypot(it.center.x - pos.x, it.center.y - pos.y) < cellPx * 1.2f }
                            ?.let { tap(it.token) }
                    }
                },
        ) {
            Canvas(Modifier.size(boardSize)) {
                drawBoard(cellPx, state.players)
                for (t in layout.sortedBy { it.color == state.currentColor && it.token in movable }) {
                    val highlight = t.color == state.currentColor && t.token in movable
                    drawToken(t.center, t.radius, t.color, if (highlight) pulse else null)
                }
            }
            // TalkBack: the tokens that can move.
            for (t in layout.filter { it.color == state.currentColor && it.token in movable }) {
                val r = cellPx * 0.6f
                Box(
                    Modifier
                        .offset { IntOffset((t.center.x - r).roundToInt(), (t.center.y - r).roundToInt()) }
                        .size(with(LocalDensity.current) { (r * 2).toDp() })
                        .clearAndSetSemantics {
                            contentDescription = "${t.color.label} token ${t.token + 1}, can move"
                            onClick { tap(t.token); true }
                        },
                )
            }
        }
    }
}

private data class TokenSpot(val color: LudoColor, val token: Int, val center: Offset, val radius: Float)

/** Works out where every token is drawn, sharing squares neatly when several stand together. */
private fun tokenCenters(state: LudoState, cell: Float, anim: LudoAnimation?, t: Float): List<TokenSpot> {
    fun cellCenter(c: Cell) = Offset((c.col + 0.5f) * cell, (c.row + 0.5f) * cell)
    fun yardSlot(color: LudoColor, i: Int): Offset {
        val corner = LudoGeometry.yardCorner(color)
        return Offset((corner.col + 2f + (i % 2) * 2f) * cell, (corner.row + 2f + (i / 2) * 2f) * cell)
    }
    fun homeSpot(color: LudoColor, i: Int): Offset {
        val c = Offset(7.5f * cell, 7.5f * cell)
        val toward = when (color) {
            LudoColor.RED -> Offset(-1f, 0f); LudoColor.GREEN -> Offset(0f, -1f)
            LudoColor.YELLOW -> Offset(1f, 0f); LudoColor.BLUE -> Offset(0f, 1f)
        }
        val side = Offset(-toward.y, toward.x)
        return c + toward * (0.9f * cell) + side * ((i - 1.5f) * 0.32f * cell)
    }
    fun positionOf(color: LudoColor, i: Int, progress: Int): Offset = when (progress) {
        LudoGeometry.YARD -> yardSlot(color, i)
        LudoGeometry.HOME -> homeSpot(color, i)
        else -> cellCenter(LudoGeometry.cellOf(color, progress)!!)
    }

    val spots = mutableListOf<TokenSpot>()
    val byCell = mutableMapOf<Offset, MutableList<Pair<LudoColor, Int>>>()
    for (color in state.players) {
        state.tokens.getValue(color).forEachIndexed { i, p ->
            if (anim != null && anim.color == color && anim.token == i && t < 1f) {
                // Moving token: walk the squares between from and to.
                val stops = if (anim.from == LudoGeometry.YARD) listOf(yardSlot(color, i), positionOf(color, i, 0))
                else (anim.from..anim.to).map { positionOf(color, i, it) }
                val x = t * (stops.size - 1)
                val k = x.toInt().coerceAtMost(stops.size - 2)
                val pos = stops[k] + (stops[k + 1] - stops[k]) * (x - k)
                // A little hop up on each step.
                val lift = sin((x - k) * PI).toFloat() * cell * 0.25f
                spots += TokenSpot(color, i, pos - Offset(0f, lift), cell * 0.38f)
            } else when (p) {
                LudoGeometry.YARD -> spots += TokenSpot(color, i, yardSlot(color, i), cell * 0.55f)
                LudoGeometry.HOME -> spots += TokenSpot(color, i, homeSpot(color, i), cell * 0.2f)
                else -> byCell.getOrPut(positionOf(color, i, p)) { mutableListOf() } += color to i
            }
        }
    }
    for ((center, tokens) in byCell) {
        if (tokens.size == 1) {
            spots += TokenSpot(tokens[0].first, tokens[0].second, center, cell * 0.4f)
        } else {
            tokens.forEachIndexed { n, (color, i) ->
                val dx = if (n % 2 == 0) -0.2f else 0.2f
                val dy = if (tokens.size <= 2) 0f else if (n < 2) -0.2f else 0.2f
                spots += TokenSpot(color, i, center + Offset(dx * cell, dy * cell), cell * 0.28f)
            }
        }
    }
    return spots
}

private fun DrawScope.drawBoard(cell: Float, players: List<LudoColor>) {
    drawRect(Color(0xFFFFFCF4))
    val line = Color(0xFF8C8C8C)
    // Yards: coloured corners with white inner squares and four token slots.
    for (color in LudoColor.entries) {
        val c = LudoGeometry.yardCorner(color)
        val tl = Offset(c.col * cell, c.row * cell)
        val active = color in players
        drawRect(if (active) color.main() else color.main().copy(alpha = 0.35f), tl, Size(6 * cell, 6 * cell))
        drawRoundRect(Color.White, tl + Offset(cell, cell), Size(4 * cell, 4 * cell), CornerRadius(cell * 0.5f))
        for (i in 0..3) {
            val center = Offset((c.col + 2f + (i % 2) * 2f) * cell, (c.row + 2f + (i / 2) * 2f) * cell)
            drawCircle(color.light(), cell * 0.62f, center)
            drawCircle(color.main(), cell * 0.62f, center, style = Stroke(cell * 0.06f))
        }
    }
    // Track squares, with start squares coloured and star squares marked as safe.
    LudoGeometry.TRACK.forEachIndexed { i, c ->
        val tl = Offset(c.col * cell, c.row * cell)
        val startOf = LudoColor.entries.firstOrNull { it.startIndex == i }
        drawRect(startOf?.main() ?: Color.White, tl, Size(cell, cell))
        drawRect(line, tl, Size(cell, cell), style = Stroke(cell * 0.03f))
        if (i in LudoGeometry.STARS) drawStar(tl + Offset(cell / 2, cell / 2), cell * 0.36f, Color(0xFF6E6E6E))
        if (startOf != null) drawStar(tl + Offset(cell / 2, cell / 2), cell * 0.36f, Color.White)
    }
    // Home columns.
    for (color in LudoColor.entries) {
        for (c in LudoGeometry.homeColumn(color)) {
            val tl = Offset(c.col * cell, c.row * cell)
            drawRect(color.main(), tl, Size(cell, cell))
            drawRect(line, tl, Size(cell, cell), style = Stroke(cell * 0.03f))
        }
    }
    // Centre: four triangles pointing home.
    val mid = Offset(7.5f * cell, 7.5f * cell)
    fun tri(a: Offset, b: Offset, color: LudoColor) {
        drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(mid.x, mid.y); close() }, color.main())
    }
    val tl = Offset(6 * cell, 6 * cell); val tr = Offset(9 * cell, 6 * cell)
    val bl = Offset(6 * cell, 9 * cell); val br = Offset(9 * cell, 9 * cell)
    tri(tl, bl, LudoColor.RED); tri(tl, tr, LudoColor.GREEN); tri(tr, br, LudoColor.YELLOW); tri(bl, br, LudoColor.BLUE)
    drawRect(line, tl, Size(3 * cell, 3 * cell), style = Stroke(cell * 0.03f))
}

private fun DrawScope.drawStar(center: Offset, r: Float, color: Color) {
    val path = Path()
    for (k in 0 until 10) {
        val radius = if (k % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + k * PI / 5
        val p = Offset(center.x + (radius * cos(a)).toFloat(), center.y + (radius * sin(a)).toFloat())
        if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color, style = Stroke(r * 0.16f))
}

/** A round token: coloured body, white ring and dark outline; [pulse] (0.6–1) draws a ring for "can move". */
private fun DrawScope.drawToken(center: Offset, r: Float, color: LudoColor, pulse: Float?) {
    if (pulse != null) {
        drawCircle(Color(0xFF111111), r * (1.25f + 0.25f * pulse), center, style = Stroke(r * 0.22f))
        drawCircle(Color.White, r * (1.25f + 0.25f * pulse), center, style = Stroke(r * 0.1f))
    }
    drawCircle(Color(0x55000000), r, center + Offset(r * 0.08f, r * 0.12f))
    drawCircle(color.main(), r, center)
    drawCircle(Color.White, r * 0.62f, center, style = Stroke(r * 0.2f))
    drawCircle(Color(0xFF1A1A1A), r, center, style = Stroke(r * 0.12f))
}
