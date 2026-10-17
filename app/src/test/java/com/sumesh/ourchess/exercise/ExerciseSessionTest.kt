package com.sumesh.ourchess.exercise

import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.exercise.ExercisePhase
import com.sumesh.ourchess.ui.exercise.ExerciseSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseSessionTest {

    private fun ExerciseSession.move(from: String, to: String) {
        onSquareTapped(Square.parse(from))
        onSquareTapped(Square.parse(to))
    }

    // Knight fork lesson: Nc7+, Ke7, Nxa8
    private val forkFen = "r3k3/8/8/3N4/8/8/8/4K3 w - - 0 1"
    private val forkSolution = listOf("d5c7", "e8e7", "c7a8")

    @Test
    fun solvingWithRepliesFinishesUnaided() = runTest {
        var result: Boolean? = null
        val s = ExerciseSession(this, forkFen, null, forkSolution, onFinished = { result = it })
        s.move("d5", "c7")
        assertEquals(ExercisePhase.OPPONENT_REPLY, s.state.phase)
        advanceUntilIdle()
        assertEquals(ExercisePhase.YOUR_TURN, s.state.phase)
        s.move("c7", "a8")
        assertEquals(ExercisePhase.SOLVED, s.state.phase)
        assertEquals(true, result)
    }

    @Test
    fun wrongMoveIsTakenBack() = runTest {
        val s = ExerciseSession(this, forkFen, null, forkSolution)
        s.move("d5", "b6")
        assertEquals(ExercisePhase.WRONG_MOVE, s.state.phase)
        advanceUntilIdle()
        assertEquals(ExercisePhase.YOUR_TURN, s.state.phase)
        assertNull(s.state.lastMove)
    }

    @Test
    fun hintShowsTheMoveAndCountsAsHelp() = runTest {
        var result: Boolean? = null
        val s = ExerciseSession(this, forkFen, null, forkSolution, onFinished = { result = it })
        s.hint()
        assertEquals(Square.parse("d5"), s.state.selected)
        assertEquals(setOf(Square.parse("c7")), s.state.targets)
        s.onSquareTapped(Square.parse("c7"))
        advanceUntilIdle()
        s.move("c7", "a8")
        assertEquals(false, result)
    }

    @Test
    fun puzzleSetupMoveThenShowSolution() = runTest {
        var result: Boolean? = null
        // Lichess-style: opponent plays f3d4, then the player mates with d5g2.
        val s = ExerciseSession(
            this, "r2r2k1/pb3ppp/1p2p3/3q4/1P1b1P2/P4N2/4B1PP/2RQ1RK1 w - - 0 17", "f3d4", listOf("d5g2"),
            onFinished = { result = it },
        )
        assertEquals(ExercisePhase.OPPONENT_SETUP, s.state.phase)
        advanceUntilIdle()
        assertEquals(ExercisePhase.YOUR_TURN, s.state.phase)
        assertTrue(s.state.pieces.isNotEmpty())
        s.showSolution()
        advanceUntilIdle()
        assertEquals(ExercisePhase.SOLUTION_SHOWN, s.state.phase)
        assertEquals(false, result)
    }

    @Test
    fun alternativeCheckmateIsAccepted() = runTest {
        // Back-rank: the lesson expects Ra8#, but Rb8# would mate too in this variant.
        val s = ExerciseSession(this, "6k1/5ppp/8/8/8/8/5PPP/1R4K1 w - - 0 1", null, listOf("b1b8"))
        s.move("b1", "b8")
        assertEquals(ExercisePhase.SOLVED, s.state.phase)
    }
}
