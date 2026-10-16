package com.sumesh.ourchess.engine

import com.sumesh.ourchess.game.ChessGame
import com.sumesh.ourchess.game.GameStatus
import com.sumesh.ourchess.game.Side
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Runs the real Stockfish (desktop build downloaded by Gradle, not shipped in the app).
 * Skipped where that build can't run (anything but Linux x86-64).
 */
class StockfishEngineTest {

    @Before
    fun requireBinary() = assumeTrue("Stockfish binary missing at $BINARY", BINARY.canExecute())

    @Test
    fun everyLevelPlaysALegalMove() = runBlocking {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        for (level in 1..5) {
            val engine = StockfishEngine(uci, EngineLevels.settings(level), EngineLevels.randomMoveChance(level))
            val move = engine.bestMove(fen)
            assertNotNull("level $level", move)
            assertTrue("level $level played illegal $move", ChessGame(fen).makeUciMove(move!!))
        }
    }

    @Test
    fun hintFindsMateInOne() = runBlocking {
        assertEquals("a1a8", StockfishEngine(uci, EngineLevels.hint).bestMove("6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1"))
    }

    @Test
    fun noMoveWhenGameIsOver() = runBlocking {
        // Black is checkmated.
        assertEquals(null, uci.bestMove("R5k1/5ppp/8/8/8/8/5PPP/6K1 b - - 1 1", EngineLevels.hint))
    }

    @Test
    fun cancelledSearchDoesNotBreakTheEngine() = runBlocking {
        val slow = async { uci.bestMove(ChessGame.START_FEN, SearchSettings(skill = 20, moveTimeMs = 500)) }
        slow.cancel()
        val move = uci.bestMove(ChessGame.START_FEN, EngineLevels.settings(3))
        assertTrue(ChessGame().makeUciMove(move!!))
    }

    @Test
    fun level4ClearlyBeatsLevel1() = runBlocking {
        var level4Points = 0.0
        repeat(2) { game ->
            val strongSide = if (game == 0) Side.WHITE else Side.BLACK
            val strong = StockfishEngine(uci, EngineLevels.settings(4))
            val weak = StockfishEngine(uci, EngineLevels.settings(1), EngineLevels.randomMoveChance(1), Random(game))
            val board = ChessGame()
            while (!board.status().isOver && board.moveCount < 300) {
                val engine = if (board.sideToMove == strongSide) strong else weak
                assertTrue(board.makeUciMove(engine.bestMove(board.fen)!!))
            }
            level4Points += when {
                board.status() == GameStatus.CHECKMATE && board.sideToMove != strongSide -> 1.0
                board.status() == GameStatus.CHECKMATE -> 0.0
                else -> 0.5
            }
        }
        assertTrue("level 4 scored only $level4Points / 2", level4Points >= 1.5)
    }

    companion object {
        private val BINARY = File(System.getProperty("stockfish.desktop") ?: "missing")
        private val uci by lazy { UciProcess(BINARY.path) }

        @JvmStatic
        @AfterClass
        fun shutdown() = uci.close()
    }
}
