package com.sumesh.ourchess.engine

/** A computer opponent. Positions and moves use standard FEN / UCI notation. */
interface ChessEngine {
    /** Returns the move to play in [fen] as UCI (e.g. "e2e4"), or null if there is none. */
    suspend fun bestMove(fen: String): String?
}
