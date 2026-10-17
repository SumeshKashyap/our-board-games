package com.sumesh.ourchess.checkers

import com.sumesh.ourchess.game.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CheckersTest {

    private fun pos(toMove: CheckersColor, vararg pieces: Pair<String, String>) = CheckersPosition(
        pieces.associate { (sq, kind) ->
            Square.parse(sq) to CheckersPiece(if (kind[0] == 'w') CheckersColor.WHITE else CheckersColor.BLACK, king = kind.length > 1)
        },
        toMove,
    )

    private fun CheckersPosition.notations() = legalMoves().map { it.notation }.toSet()

    @Test
    fun startingPosition() {
        val start = CheckersPosition.START
        assertEquals(24, start.pieces.size)
        assertTrue(start.pieces.keys.all { it.isDark() })
        assertEquals(setOf("a3-b4", "c3-b4", "c3-d4", "e3-d4", "e3-f4", "g3-f4", "g3-h4"), start.notations())
    }

    @Test
    fun captureIsCompulsory() {
        val p = pos(CheckersColor.WHITE, "c3" to "w", "a3" to "w", "d4" to "b")
        assertEquals(setOf("c3:e5"), p.notations())
    }

    @Test
    fun menCaptureBackwards() {
        val p = pos(CheckersColor.WHITE, "e5" to "w", "d4" to "b")
        assertEquals(setOf("e5:c3"), p.notations())
    }

    @Test
    fun multiJumpMustContinue() {
        val p = pos(CheckersColor.WHITE, "c3" to "w", "d4" to "b", "f6" to "b")
        assertEquals(setOf("c3:e5:g7"), p.notations())
        val after = p.apply(p.legalMoves().single())
        assertEquals(1, after.pieces.size)
    }

    @Test
    fun manCrownedMidCaptureContinuesAsKing() {
        // b6 takes c7 landing on d8 (crowned), then as a flying king takes g5 from a distance.
        val p = pos(CheckersColor.WHITE, "b6" to "w", "c7" to "b", "g5" to "b")
        assertEquals(setOf("b6:d8:h4"), p.notations())
        val after = p.apply(p.legalMoves().single())
        assertEquals(CheckersPiece(CheckersColor.WHITE, king = true), after.pieces[Square.parse("h4")])
    }

    @Test
    fun manReachingLastRowBecomesKing() {
        val p = pos(CheckersColor.WHITE, "c7" to "w", "a1" to "b")
        val after = p.apply(p.legalMoves().first { it.to == Square.parse("d8") })
        assertTrue(after.pieces.getValue(Square.parse("d8")).king)
    }

    @Test
    fun flyingKingMovesAnyDistance() {
        val p = pos(CheckersColor.WHITE, "a1" to "wK", "h2" to "b")
        assertEquals(7, p.legalMoves().size) // b2 ... h8
    }

    @Test
    fun kingCapturesFromDistanceWithChoiceOfLanding() {
        val p = pos(CheckersColor.WHITE, "a1" to "wK", "d4" to "b")
        assertEquals(setOf("a1:e5", "a1:f6", "a1:g7", "a1:h8"), p.notations())
    }

    @Test
    fun kingMustLandWhereItCanKeepCapturing() {
        // Only landing on f6 allows the second capture (e7), so it's the only legal move.
        val p = pos(CheckersColor.WHITE, "a1" to "wK", "d4" to "b", "e7" to "b")
        assertEquals(setOf("a1:f6:d8"), p.notations())
    }

    @Test
    fun capturedPiecesCannotBeJumpedTwiceAndBlock() {
        // After taking d4 and f6, the king can't come back over d4 again.
        val p = pos(CheckersColor.WHITE, "c3" to "wK", "d4" to "b", "f4" to "b")
        p.legalMoves().forEach { m -> assertEquals(m.captured.size, m.captured.toSet().size) }
    }

    @Test
    fun noMovesMeansLoss() {
        val game = CheckersGame(pos(CheckersColor.BLACK, "c3" to "w"))
        assertEquals(CheckersStatus.WHITE_WINS, game.status())
        val blocked = CheckersGame(pos(CheckersColor.WHITE, "a1" to "w", "b2" to "b", "c3" to "b"))
        assertEquals(CheckersStatus.BLACK_WINS, blocked.status())
    }

    @Test
    fun drawByRepetition() {
        // Kings on separate diagonals (on the same long diagonal a capture would be compulsory).
        val game = CheckersGame(pos(CheckersColor.WHITE, "a1" to "wK", "h2" to "bK"))
        repeat(2) {
            assertTrue(game.makeMove("a1-b2")); assertTrue(game.makeMove("h2-g1"))
            assertTrue(game.makeMove("b2-a1")); assertTrue(game.makeMove("g1-h2"))
        }
        assertEquals(CheckersStatus.DRAW, game.status())
    }

    @Test
    fun undoAndLostPieces() {
        val game = CheckersGame(pos(CheckersColor.WHITE, "c3" to "w", "d4" to "b", "h8" to "b"))
        assertTrue(game.makeMove("c3:e5"))
        assertEquals(1, game.lost(CheckersColor.BLACK).size)
        game.undo()
        assertEquals(0, game.lost(CheckersColor.BLACK).size)
        assertFalse(game.makeMove("c3-b4")) // capture is compulsory
    }

    @Test
    fun keyRoundTrip() {
        val p = pos(CheckersColor.BLACK, "a1" to "wK", "c3" to "w", "h8" to "b", "f6" to "bK")
        assertEquals(p, CheckersPosition.fromKey(p.key()))
        assertEquals(CheckersPosition.START, CheckersPosition.fromKey(CheckersPosition.START.key()))
    }

    @Test
    fun aiTakesTheWinningShot() {
        // White can sacrifice nothing and simply take two; the AI must find the double capture line.
        val p = pos(CheckersColor.WHITE, "c3" to "w", "d4" to "b", "f6" to "b", "h8" to "b", "a7" to "b")
        assertEquals("c3:e5:g7", CheckersAi(Random(1)).bestMove(p, CheckersLevels.strength(5))?.notation)
    }

    @Test
    fun strongerLevelBeatsWeakest() {
        var points = 0.0
        repeat(2) { g ->
            val strongColor = if (g == 0) CheckersColor.WHITE else CheckersColor.BLACK
            val game = CheckersGame()
            val ai = CheckersAi(Random(g))
            while (!game.status().isOver && game.moveCount < 200) {
                val level = if (game.toMove == strongColor) 4 else 1
                assertTrue(game.makeMove(ai.bestMove(game.position, CheckersLevels.strength(level))!!))
            }
            points += when (game.status()) {
                CheckersStatus.WHITE_WINS -> if (strongColor == CheckersColor.WHITE) 1.0 else 0.0
                CheckersStatus.BLACK_WINS -> if (strongColor == CheckersColor.BLACK) 1.0 else 0.0
                else -> 0.5
            }
        }
        assertTrue("level 4 scored $points / 2", points >= 1.5)
    }
}
