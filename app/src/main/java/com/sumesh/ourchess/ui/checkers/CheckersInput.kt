package com.sumesh.ourchess.ui.checkers

import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersMove
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.checkers.CheckersPosition
import com.sumesh.ourchess.game.Square

/**
 * Turns taps into a checkers move: tap a piece, then where it goes. For a multi-capture, tap each landing square
 * in turn — or tap the final square directly when that identifies the move.
 */
class CheckersInput {
    /** The squares chosen so far, starting with the selected piece. Empty = nothing selected. */
    var path: List<Square> = emptyList()
        private set

    val selected: Square? get() = path.firstOrNull()

    fun clear() {
        path = emptyList()
    }

    /** Next squares that can be tapped. */
    fun targets(legal: List<CheckersMove>): Set<Square> =
        if (path.isEmpty()) emptySet() else candidates(legal).map { it.path[path.size] }.toSet()

    /** Handles a tap. Returns the finished move once one is fully chosen. */
    fun tap(square: Square, position: CheckersPosition, side: CheckersColor): CheckersMove? {
        val legal = position.legalMoves()
        val own = position.pieces[square]?.let { it.color == side } == true
        if (path.isEmpty()) {
            if (own) path = listOf(square)
            return null
        }
        val candidates = candidates(legal)
        val extended = path + square
        val continuing = candidates.filter { it.path.size >= extended.size && it.path.subList(0, extended.size) == extended }
        when {
            continuing.isNotEmpty() -> {
                continuing.firstOrNull { it.path.size == extended.size }?.let { path = emptyList(); return it }
                path = extended
            }
            // Tapping the final square of exactly one possible move completes it.
            candidates.count { it.to == square } == 1 -> {
                val move = candidates.first { it.to == square }
                path = emptyList()
                return move
            }
            path.size == 1 && square == path.first() -> path = emptyList()
            path.size == 1 && own -> path = listOf(square)
            else -> path = emptyList()
        }
        return null
    }

    private fun candidates(legal: List<CheckersMove>): List<CheckersMove> =
        legal.filter { it.path.size > path.size && it.path.subList(0, path.size) == path }
}

/** "c3 to d4", "c3 takes 2 pieces, lands on g7", "…, becomes a king". */
fun describeCheckersMove(position: CheckersPosition, move: CheckersMove): String {
    val piece: CheckersPiece = position.pieces.getValue(move.from)
    val text = StringBuilder()
    when (move.captured.size) {
        0 -> text.append("${move.from.name} to ${move.to.name}")
        1 -> text.append("${move.from.name} takes ${move.captured.single().name}, lands on ${move.to.name}")
        else -> text.append("${move.from.name} takes ${move.captured.size} pieces, lands on ${move.to.name}")
    }
    if (!piece.king && move.path.any { it.rank == piece.color.crownRank }) text.append(", becomes a king")
    return text.toString()
}
