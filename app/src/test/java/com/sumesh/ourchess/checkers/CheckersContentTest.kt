package com.sumesh.ourchess.checkers

import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.checkers.CheckersInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CheckersContentTest {

    @Test
    fun everyLessonStepIsLegal() {
        for (lesson in CheckersLessons.all) for (step in lesson.steps) {
            val game = CheckersGame(CheckersPosition.fromKey(step.key.replace(';', ',')))
            for (m in step.solution) assertTrue("${lesson.id}: illegal $m", game.makeMove(m))
        }
    }

    @Test
    fun everyPuzzleIsLegalAndWinsMaterial() {
        val puzzles = CheckersPuzzles.parse(File("src/main/assets/checkers_puzzles.csv").readText())
        assertTrue("only ${puzzles.size} puzzles", puzzles.size >= 30)
        for (level in 1..5) assertTrue("no level $level puzzles", puzzles.any { it.level == level })
        for (p in puzzles) {
            val side = p.position.toMove
            fun material(pos: CheckersPosition) = pos.pieces.values.sumOf { (if (it.king) 3 else 1) * if (it.color == side) 1 else -1 }
            val game = CheckersGame(p.position)
            for (m in p.solution) assertTrue("${p.id}: illegal $m", game.makeMove(m))
            assertTrue("${p.id} doesn't win material", material(game.position) - material(p.position) >= 2)
        }
    }

    @Test
    fun tappingAMultiCaptureHopByHopOrDirectly() {
        val pos = CheckersPosition.fromKey("W:wc3,bd4,bf6,ba7")
        val hopByHop = CheckersInput()
        assertNull(hopByHop.tap(Square.parse("c3"), pos, CheckersColor.WHITE))
        assertEquals(setOf(Square.parse("e5")), hopByHop.targets(pos.legalMoves()))
        assertNull(hopByHop.tap(Square.parse("e5"), pos, CheckersColor.WHITE))
        assertEquals("c3:e5:g7", hopByHop.tap(Square.parse("g7"), pos, CheckersColor.WHITE)?.notation)

        val direct = CheckersInput()
        direct.tap(Square.parse("c3"), pos, CheckersColor.WHITE)
        assertEquals("c3:e5:g7", direct.tap(Square.parse("g7"), pos, CheckersColor.WHITE)?.notation)
    }
}
