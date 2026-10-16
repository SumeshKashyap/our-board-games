package com.sumesh.ourchess.content

import com.sumesh.ourchess.game.ChessGame
import com.sumesh.ourchess.game.GameStatus
import com.sumesh.ourchess.game.Piece
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.puzzles.PuzzleRepository
import com.sumesh.ourchess.tutorials.Lessons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every lesson and bundled puzzle must be playable with legal moves. */
class ContentTest {

    @Test
    fun allLessonMovesAreLegal() {
        for (lesson in Lessons.all) for (step in lesson.steps) {
            val game = ChessGame(step.fen)
            for (uci in step.solution) assertTrue("${lesson.id}: illegal $uci", game.makeUciMove(uci))
        }
    }

    @Test
    fun lessonOutcomes() {
        fun finalGame(id: String) = Lessons.byId(id).steps.last().let { s -> ChessGame(s.fen).also { g -> s.solution.forEach(g::makeUciMove) } }
        assertEquals(GameStatus.CHECKMATE, finalGame("back_rank").status())
        assertEquals(Piece(Side.WHITE, PieceType.QUEEN), finalGame("promotion").pieceAt(Square.parse("e8")))
        assertTrue(Lessons.byId("pin").steps[0].let { ChessGame(it.fen).legalMoves().isNotEmpty() })
    }

    @Test
    fun allPuzzlesAreValid() {
        val puzzles = PuzzleRepository.parse(File("src/main/assets/puzzles.csv").readText())
        assertEquals(600, puzzles.size)
        for (level in 1..5) assertEquals(120, puzzles.count { it.level == level })
        for (p in puzzles) {
            val game = ChessGame(p.fen)
            for (uci in p.moves) assertTrue("puzzle ${p.id}: illegal $uci", game.makeUciMove(uci))
            if (p.goal.startsWith("Checkmate")) assertEquals("puzzle ${p.id}", GameStatus.CHECKMATE, game.status())
        }
    }
}
