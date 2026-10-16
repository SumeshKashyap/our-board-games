package com.sumesh.ourchess.engine

import android.content.Context
import android.util.Log
import com.sumesh.ourchess.game.ChessGame
import java.io.File

/** Engine strength for each difficulty level (1 = easiest, 5 = hardest). */
object EngineLevels {
    fun settings(level: Int): SearchSettings = when (level) {
        1 -> SearchSettings(skill = 0, depth = 1)
        2 -> SearchSettings(skill = 3, depth = 2)
        3 -> SearchSettings(skill = 7, depth = 5)
        4 -> SearchSettings(skill = 12, depth = 9)
        else -> SearchSettings(skill = 17, moveTimeMs = 700)
    }

    fun randomMoveChance(level: Int): Double = when (level) {
        1 -> 0.25
        2 -> 0.08
        else -> 0.0
    }

    /** Hints should be genuinely good moves, whatever the level. */
    val hint = SearchSettings(skill = 20, moveTimeMs = 600)
}

/** Provides the Stockfish engine bundled in the app (as libstockfish.so), or a simple fallback. */
object Engines {
    private var uci: UciProcess? = null

    fun init(context: Context) {
        if (uci != null) return
        val binary = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")
        if (binary.canExecute()) {
            uci = UciProcess(binary.path)
        } else {
            Log.w("Engines", "Stockfish not available for this device; using the simple engine")
        }
    }

    /** Starts the engine ahead of time so the first move isn't slow. */
    suspend fun warmUp() {
        uci?.bestMove(ChessGame.START_FEN, SearchSettings(skill = 0, depth = 1))
    }

    fun forLevel(level: Int): ChessEngine = uci?.let {
        StockfishEngine(it, EngineLevels.settings(level), EngineLevels.randomMoveChance(level))
    } ?: RandomMoveEngine()

    fun forHints(): ChessEngine = uci?.let { StockfishEngine(it, EngineLevels.hint) } ?: RandomMoveEngine(thinkTimeMs = 0)
}
