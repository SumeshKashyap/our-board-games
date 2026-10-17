package com.sumesh.ourchess.checkers

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Finds checkers puzzles in computer-vs-computer games and writes app/src/main/assets/checkers_puzzles.csv.
 * Not part of the normal test run. To regenerate:  MINE_CHECKERS=1 ./gradlew testDebugUnitTest --tests '*CheckersPuzzleMiner*'
 *
 * A puzzle is kept only if the player's best move is clearly better than every alternative (unique answer),
 * every later move the player must find is unique too, and the line wins at least two pieces.
 */
class CheckersPuzzleMiner {

    private val ai = CheckersAi(Random(7))

    private fun material(p: CheckersPosition, side: CheckersColor) =
        p.pieces.values.sumOf { (if (it.king) 3 else 1) * (if (it.color == side) 1 else -1).toInt() }

    data class Found(val key: String, val solution: List<String>, val level: Int, val gain: Int, val kingMade: Boolean)

    /** The player's unique best move here, or null if several moves are about as good. */
    private fun uniqueBest(p: CheckersPosition, depth: Int): CheckersMove? {
        val scored = ai.scoreMoves(p, depth)
        if (scored.size < 2) return null
        return scored[0].first.takeIf { scored[0].second - scored[1].second >= 150 }
    }

    private fun tryPuzzle(start: CheckersPosition): Found? {
        if (start.legalMoves().size < 2) return null
        // Cheap filter first: is there a big gain at all?
        val quick = ai.scoreMoves(start, 4)
        if (quick.size < 2 || quick[0].second - quick[1].second < 150) return null
        val side = start.toMove
        val before = material(start, side)
        var pos = start
        val line = mutableListOf<CheckersMove>()
        val userMoves = mutableListOf<CheckersMove>()
        repeat(3) {
            val mine = uniqueBest(pos, 7) ?: return null
            line += mine; userMoves += mine
            pos = pos.apply(mine)
            val gain = material(pos, side) - before
            val opponentCanHitBack = pos.legalMoves().any { it.isCapture }
            if (gain >= 2 && !opponentCanHitBack) {
                val first = userMoves.first()
                val sacrifice = line.size > 1 && start.apply(first).legalMoves().all { it.isCapture }
                var level = userMoves.size + (if (!first.isCapture) 1 else 0) + (if (sacrifice) 1 else 0)
                level = level.coerceIn(1, 5)
                val kingMade = pos.pieces.values.count { it.color == side && it.king } > start.pieces.values.count { it.color == side && it.king }
                return Found(start.key(), line.map { it.notation }, level, gain, kingMade)
            }
            val reply = ai.bestMove(pos, CheckersStrength(depth = 7, randomMoveChance = 0.0, timeLimitMs = 5_000)) ?: return null
            line += reply
            pos = pos.apply(reply)
        }
        return null
    }

    @Test
    fun mine() {
        assumeTrue(System.getenv("MINE_CHECKERS") == "1")
        // Harder puzzles are rare, so fewer of them are required; stop at the targets or after 20 minutes.
        val wanted = mapOf(1 to 20, 2 to 10, 3 to 3, 4 to 3, 5 to 3)
        val deadline = System.currentTimeMillis() + 20 * 60_000
        val found = mutableMapOf<Int, MutableList<Found>>()
        val seen = mutableSetOf<String>()
        val rnd = Random(2026)
        var games = 0
        while ((1..5).any { (found[it]?.size ?: 0) < wanted.getValue(it) } && System.currentTimeMillis() < deadline) {
            games++
            val game = CheckersGame()
            while (!game.status().isOver && game.moveCount < 120) {
                val pos = game.position
                if (game.moveCount >= 6 && pos.key() !in seen) {
                    seen += pos.key()
                    tryPuzzle(pos)?.let { f ->
                        val bucket = found.getOrPut(f.level) { mutableListOf() }
                        if (bucket.size < 20) bucket += f
                    }
                }
                val strength = if (game.moveCount < 4) CheckersStrength(1, 1.0) else CheckersStrength(3, 0.15)
                val move = CheckersAi(rnd).bestMove(pos, strength) ?: break
                game.makeMove(move)
            }
            if (games % 25 == 0) println("games=$games found=" + (1..5).map { found[it]?.size ?: 0 })
        }
        val out = File("src/main/assets/checkers_puzzles.csv")
        out.printWriter().use { w ->
            w.println("id,level,position,solution,goal")
            var id = 1
            for (level in 1..5) for (f in found[level].orEmpty()) {
                val goal = "Win ${f.gain} pieces" + if (f.kingMade) " and make a king" else ""
                w.println("C${id++},$level,${f.key.replace(',', ';')},${f.solution.joinToString(" ")},$goal")
            }
        }
        println("wrote ${out.path}: " + (1..5).map { found[it]?.size ?: 0 } + " after $games games")
    }
}
