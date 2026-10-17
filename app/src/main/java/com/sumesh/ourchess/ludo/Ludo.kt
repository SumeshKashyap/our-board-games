package com.sumesh.ourchess.ludo

import kotlin.random.Random

/*
 * Ludo, with the rules most Indian families play:
 * - 2–4 players, 4 tokens each, starting in their yard.
 * - A 6 is needed to bring a token out onto its start square.
 * - Rolling a 6 gives another roll; three 6s in a row and the turn is lost.
 * - Landing on an opponent's token (not on a safe square) sends it back to its yard, and earns another roll.
 * - Safe squares: every start square and the four star squares. Tokens there can't be captured.
 * - After going round the board, a token enters its own coloured home column; reaching home needs the exact
 *   number and earns another roll.
 * - The first player with all four tokens home wins.
 */

/** Colours in turn order (clockwise). [startIndex] is the track square where that colour's tokens come out. */
enum class LudoColor(val label: String, val startIndex: Int) {
    RED("Red", 0), GREEN("Green", 13), YELLOW("Yellow", 26), BLUE("Blue", 39);

    companion object {
        /** Which colours play for 2, 3 or 4 players (two players sit opposite each other). */
        fun forPlayers(count: Int): List<LudoColor> = when (count) {
            2 -> listOf(RED, YELLOW)
            3 -> listOf(RED, GREEN, YELLOW)
            else -> entries
        }
    }
}

/** A cell on the 15×15 board grid (column, row from the top). */
data class Cell(val col: Int, val row: Int)

/** The board's shape: where each step of a token's journey is drawn. */
object LudoGeometry {
    const val YARD = -1
    /** Progress 0..50 is on the shared track, 51..55 the colour's home column, 56 is home. */
    const val LAST_TRACK = 50
    const val HOME = 56
    const val TRACK_LENGTH = 52

    /** The shared track, clockwise, starting at Red's start square. */
    val TRACK: List<Cell> = buildList {
        for (c in 1..5) add(Cell(c, 6))
        for (r in 5 downTo 0) add(Cell(6, r))
        add(Cell(7, 0))
        for (r in 0..5) add(Cell(8, r))
        for (c in 9..14) add(Cell(c, 6))
        add(Cell(14, 7))
        for (c in 14 downTo 9) add(Cell(c, 8))
        for (r in 9..14) add(Cell(8, r))
        add(Cell(7, 14))
        for (r in 14 downTo 9) add(Cell(6, r))
        for (c in 5 downTo 0) add(Cell(c, 8))
        add(Cell(0, 7))
        add(Cell(0, 6))
    }

    /** Start squares and star squares, where tokens can't be captured. */
    val SAFE: Set<Int> = LudoColor.entries.flatMap { listOf(it.startIndex, it.startIndex + 8) }.toSet()
    val STARS: Set<Int> = LudoColor.entries.map { it.startIndex + 8 }.toSet()

    fun homeColumn(color: LudoColor): List<Cell> = when (color) {
        LudoColor.RED -> (1..5).map { Cell(it, 7) }
        LudoColor.GREEN -> (1..5).map { Cell(7, it) }
        LudoColor.YELLOW -> (13 downTo 9).map { Cell(it, 7) }
        LudoColor.BLUE -> (13 downTo 9).map { Cell(7, it) }
    }

    /** The 6×6 yard corner for a colour (top-left cell). */
    fun yardCorner(color: LudoColor): Cell = when (color) {
        LudoColor.RED -> Cell(0, 0)
        LudoColor.GREEN -> Cell(9, 0)
        LudoColor.YELLOW -> Cell(9, 9)
        LudoColor.BLUE -> Cell(0, 9)
    }

    /** Track square index for a token's progress, or null when it is in the yard, home column or home. */
    fun trackIndex(color: LudoColor, progress: Int): Int? =
        if (progress in 0..LAST_TRACK) (color.startIndex + progress) % TRACK_LENGTH else null

    /** Where a token is drawn (null in the yard or home; the board draws those itself). */
    fun cellOf(color: LudoColor, progress: Int): Cell? = when (progress) {
        in 0..LAST_TRACK -> TRACK[trackIndex(color, progress)!!]
        in LAST_TRACK + 1 until HOME -> homeColumn(color)[progress - LAST_TRACK - 1]
        else -> null
    }
}

/** The whole game state. [tokens] holds each colour's 4 token progresses (-1 = yard, 56 = home). */
data class LudoState(
    val players: List<LudoColor>,
    val tokens: Map<LudoColor, List<Int>>,
    val current: Int = 0,
    /** The rolled number waiting for a token to be moved, or null when it's time to roll. */
    val dice: Int? = null,
    val sixesInRow: Int = 0,
    val winner: LudoColor? = null,
) {
    val currentColor: LudoColor get() = players[current]

    fun movableTokens(roll: Int = dice ?: 0): List<Int> = tokens.getValue(currentColor).indices.filter { canMove(it, roll) }

    private fun canMove(token: Int, roll: Int): Boolean {
        val p = tokens.getValue(currentColor)[token]
        return when (p) {
            LudoGeometry.YARD -> roll == 6
            LudoGeometry.HOME -> false
            else -> p + roll <= LudoGeometry.HOME
        }
    }

    companion object {
        fun newGame(players: List<LudoColor>): LudoState =
            LudoState(players, players.associateWith { List(4) { LudoGeometry.YARD } })
    }
}

/** What happened in one action, for the screen to show and say. */
sealed interface LudoEvent {
    data class Rolled(val color: LudoColor, val value: Int, val canMove: Boolean, val lostTurnOnThreeSixes: Boolean) : LudoEvent
    data class Moved(
        val color: LudoColor,
        val token: Int,
        val from: Int,
        val to: Int,
        val captured: List<Pair<LudoColor, Int>>,
        val extraTurn: Boolean,
    ) : LudoEvent
}

object LudoRules {

    /** Rolling the dice. Passes the turn if nothing can move, or on a third 6 in a row. */
    fun roll(state: LudoState, value: Int): Pair<LudoState, LudoEvent.Rolled> {
        require(state.dice == null && state.winner == null) { "not time to roll" }
        val sixes = if (value == 6) state.sixesInRow + 1 else 0
        if (sixes == 3) {
            return nextPlayer(state) to LudoEvent.Rolled(state.currentColor, value, canMove = false, lostTurnOnThreeSixes = true)
        }
        val rolled = state.copy(dice = value, sixesInRow = sixes)
        val canMove = rolled.movableTokens().isNotEmpty()
        val next = if (canMove) rolled else nextPlayer(rolled)
        return next to LudoEvent.Rolled(state.currentColor, value, canMove, lostTurnOnThreeSixes = false)
    }

    /** Moving one of the current player's tokens by the rolled number. */
    fun move(state: LudoState, token: Int): Pair<LudoState, LudoEvent.Moved> {
        val roll = requireNotNull(state.dice) { "roll first" }
        require(token in state.movableTokens()) { "token $token can't move $roll" }
        val color = state.currentColor
        val from = state.tokens.getValue(color)[token]
        val to = if (from == LudoGeometry.YARD) 0 else from + roll

        val tokens = state.tokens.mapValues { it.value.toMutableList() }.toMutableMap()
        tokens.getValue(color)[token] = to

        // Capture opponents on the landing square, unless it is a safe square.
        val captured = mutableListOf<Pair<LudoColor, Int>>()
        val landing = LudoGeometry.trackIndex(color, to)
        if (landing != null && landing !in LudoGeometry.SAFE) {
            for (other in state.players) {
                if (other == color) continue
                val list = tokens.getValue(other)
                for (i in list.indices) {
                    if (LudoGeometry.trackIndex(other, list[i]) == landing) {
                        list[i] = LudoGeometry.YARD
                        captured += other to i
                    }
                }
            }
        }

        val finished = tokens.getValue(color).all { it == LudoGeometry.HOME }
        val extraTurn = !finished && (roll == 6 || captured.isNotEmpty() || to == LudoGeometry.HOME)
        val moved = state.copy(tokens = tokens.mapValues { it.value.toList() }, dice = null, winner = if (finished) color else null)
        val next = when {
            finished -> moved
            extraTurn -> moved.copy(sixesInRow = if (roll == 6) moved.sixesInRow else 0)
            else -> nextPlayer(moved)
        }
        return next to LudoEvent.Moved(color, token, from, to, captured, extraTurn)
    }

    private fun nextPlayer(state: LudoState): LudoState =
        state.copy(current = (state.current + 1) % state.players.size, dice = null, sixesInRow = 0)
}

/** Computer player: weighs captures, safety, getting home and getting out, with more randomness at low levels. */
class LudoAi(private val random: Random = Random.Default) {

    fun choose(state: LudoState, level: Int): Int? {
        val options = state.movableTokens()
        if (options.isEmpty()) return null
        if (options.size == 1) return options.first()
        val randomChance = when (level) { 1 -> 0.6; 2 -> 0.3; 3 -> 0.1; else -> 0.0 }
        if (random.nextDouble() < randomChance) return options.random(random)
        return options.maxBy { score(state, it) + random.nextDouble() } // tiny noise breaks ties
    }

    fun score(state: LudoState, token: Int): Double {
        val color = state.currentColor
        val roll = state.dice ?: return 0.0
        val from = state.tokens.getValue(color)[token]
        val to = if (from == LudoGeometry.YARD) 0 else from + roll
        val landing = LudoGeometry.trackIndex(color, to)
        var s = to * 0.5
        if (to == LudoGeometry.HOME) s += 100
        if (from == LudoGeometry.YARD) s += 50
        if (from <= LudoGeometry.LAST_TRACK && to > LudoGeometry.LAST_TRACK) s += 25 // into the safe home column
        if (landing != null && landing !in LudoGeometry.SAFE && opponentsAt(state, landing) > 0) s += 80
        if (landing != null && landing in LudoGeometry.SAFE) s += 20
        if (landing != null && landing !in LudoGeometry.SAFE && threatened(state, landing)) s -= 40
        val fromIndex = LudoGeometry.trackIndex(color, from)
        if (fromIndex != null && fromIndex !in LudoGeometry.SAFE && threatened(state, fromIndex)) s += 30 // escape danger
        return s
    }

    private fun opponentsAt(state: LudoState, index: Int): Int = state.players.filter { it != state.currentColor }
        .sumOf { c -> state.tokens.getValue(c).count { LudoGeometry.trackIndex(c, it) == index } }

    /** True if an opponent token is 1–6 squares behind [index] and could still land there. */
    private fun threatened(state: LudoState, index: Int): Boolean = state.players.filter { it != state.currentColor }.any { c ->
        state.tokens.getValue(c).any { p ->
            val at = LudoGeometry.trackIndex(c, p) ?: return@any false
            val distance = (index - at + LudoGeometry.TRACK_LENGTH) % LudoGeometry.TRACK_LENGTH
            distance in 1..6 && p + distance <= LudoGeometry.LAST_TRACK
        }
    }
}
