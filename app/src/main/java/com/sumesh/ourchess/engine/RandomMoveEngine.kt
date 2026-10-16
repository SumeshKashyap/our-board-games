package com.sumesh.ourchess.engine

import com.sumesh.ourchess.game.ChessGame
import kotlinx.coroutines.delay

/** Fallback opponent when Stockfish can't run (e.g. on an x86 emulator): prefers captures, otherwise any legal move. */
class RandomMoveEngine(private val thinkTimeMs: Long = 700) : ChessEngine {
    override suspend fun bestMove(fen: String): String? {
        delay(thinkTimeMs)
        val game = ChessGame(fen)
        val moves = game.legalMoves()
        val captures = moves.filter { game.pieceAt(it.to) != null }
        return (captures.ifEmpty { moves }).randomOrNull()?.uci
    }
}
