package com.sumesh.ourchess.engine

import com.sumesh.ourchess.game.ChessGame
import kotlin.random.Random

/**
 * Stockfish playing at a chosen strength.
 * [randomMoveChance] makes the lowest levels occasionally play an aimless move,
 * since even Stockfish's weakest skill setting is too strong for a true beginner.
 */
class StockfishEngine(
    private val uci: UciProcess,
    private val settings: SearchSettings,
    private val randomMoveChance: Double = 0.0,
    private val random: Random = Random.Default,
) : ChessEngine {
    override suspend fun bestMove(fen: String): String? {
        if (randomMoveChance > 0 && random.nextDouble() < randomMoveChance) {
            ChessGame(fen).legalMoves().randomOrNull(random)?.let { return it.uci }
        }
        return uci.bestMove(fen, settings)
    }
}
