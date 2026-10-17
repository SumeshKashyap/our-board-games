package com.sumesh.ourchess.checkers

import kotlin.random.Random

/** Search strength for each difficulty level (1 = easiest, 5 = hardest). */
data class CheckersStrength(val depth: Int, val randomMoveChance: Double, val timeLimitMs: Long = 1_500)

object CheckersLevels {
    fun strength(level: Int): CheckersStrength = when (level) {
        1 -> CheckersStrength(depth = 1, randomMoveChance = 0.35)
        2 -> CheckersStrength(depth = 2, randomMoveChance = 0.12)
        3 -> CheckersStrength(depth = 4, randomMoveChance = 0.0)
        4 -> CheckersStrength(depth = 6, randomMoveChance = 0.0)
        else -> CheckersStrength(depth = 9, randomMoveChance = 0.0)
    }

    /** Hints and puzzle checks use a strong search. */
    val hint = CheckersStrength(depth = 8, randomMoveChance = 0.0)
}

/**
 * A compact alpha-beta searcher. Scores are from the point of view of the side to move.
 * Forced moves (only one legal move, common with compulsory captures) don't use up depth,
 * so combinations are seen through.
 */
class CheckersAi(private val random: Random = Random.Default) {

    fun bestMove(position: CheckersPosition, strength: CheckersStrength): CheckersMove? {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()
        if (strength.randomMoveChance > 0 && random.nextDouble() < strength.randomMoveChance) return moves.random(random)

        val deadline = System.currentTimeMillis() + strength.timeLimitMs
        var best = moves.first()
        // Iterative deepening: each finished depth gives a usable answer if time runs out.
        for (depth in 1..strength.depth) {
            var bestScore = -INF
            var bestAtDepth = best
            var completed = true
            // Search the previous depth's best move first, for better pruning.
            for (move in listOf(best) + (moves - best)) {
                val reply = search(position.apply(move), depth - 1, -INF, -bestScore, deadline, 1)
                if (reply == null) {
                    completed = false
                    break
                }
                val score = -reply
                if (score > bestScore) {
                    bestScore = score
                    bestAtDepth = move
                }
            }
            if (!completed) break
            best = bestAtDepth
            if (bestScore >= WIN - 100) break // a forced win was found
        }
        return best
    }

    /** Every legal move with its score (for the side to move) after a full-width search to [depth]. */
    fun scoreMoves(position: CheckersPosition, depth: Int): List<Pair<CheckersMove, Int>> =
        position.legalMoves().map { m -> m to -(search(position.apply(m), depth - 1, -INF, INF, Long.MAX_VALUE, 1) ?: 0) }
            .sortedByDescending { it.second }

    private fun search(position: CheckersPosition, depth: Int, alphaIn: Int, beta: Int, deadline: Long, ply: Int): Int? {
        if (System.currentTimeMillis() > deadline) return null
        val moves = position.legalMoves()
        if (moves.isEmpty()) return -WIN + ply // no moves: the side to move has lost (sooner is worse)
        val forced = moves.size == 1 || moves.first().isCapture
        if (depth <= 0 && !forced) return evaluate(position)
        if (ply > MAX_PLY) return evaluate(position)

        var alpha = alphaIn
        val nextDepth = if (forced) depth else depth - 1
        for (move in moves.sortedByDescending { it.captured.size }) {
            val score = -(search(position.apply(move), nextDepth, -beta, -alpha, deadline, ply + 1) ?: return null)
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }
        return alpha
    }

    private fun evaluate(position: CheckersPosition): Int {
        var score = 0
        for ((sq, p) in position.pieces) {
            var v = if (p.king) KING else MAN
            if (!p.king) {
                val advance = if (p.color == CheckersColor.WHITE) sq.rank else 7 - sq.rank
                v += advance * 6
                // Keeping the back row guards against enemy kings.
                if (advance == 0) v += 8
            }
            // Central squares are stronger.
            if (sq.file in 2..5 && sq.rank in 2..5) v += 10
            score += if (p.color == position.toMove) v else -v
        }
        return score
    }

    companion object {
        const val MAN = 100
        const val KING = 300
        const val WIN = 100_000
        private const val INF = 1_000_000
        private const val MAX_PLY = 60
    }
}
