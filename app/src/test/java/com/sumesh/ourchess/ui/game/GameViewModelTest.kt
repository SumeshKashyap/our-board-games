package com.sumesh.ourchess.ui.game

import com.sumesh.ourchess.data.SavedGame
import com.sumesh.ourchess.engine.ChessEngine
import com.sumesh.ourchess.game.GameStatus
import com.sumesh.ourchess.game.Move
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.game.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class ScriptedEngine(vararg moves: String) : ChessEngine {
        private val queue = ArrayDeque(moves.toList())
        override suspend fun bestMove(fen: String): String? = queue.removeFirstOrNull()
    }

    private fun GameViewModel.move(from: String, to: String) {
        onSquareTapped(Square.parse(from))
        onSquareTapped(Square.parse(to))
    }

    @Test
    fun resumesSavedGameAndSavesEveryMove() = runTest(dispatcher) {
        val saves = mutableListOf<SavedGame>()
        val vm = GameViewModel(
            engine = ScriptedEngine("b8c6"),
            saved = SavedGame(Side.WHITE, listOf("e2e4", "e7e5")),
            onSave = { saves += it },
        )
        assertEquals(Move.parseUci("e7e5"), vm.state.lastMove)
        assertTrue(vm.state.isHumanTurn)

        vm.move("g1", "f3")
        advanceUntilIdle()
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6"), saves.last().moves)
    }

    @Test
    fun resumingWhenComputerToMoveLetsItPlay() = runTest(dispatcher) {
        val vm = GameViewModel(engine = ScriptedEngine("e7e5"), saved = SavedGame(Side.WHITE, listOf("e2e4")))
        assertTrue(vm.state.computerThinking)
        advanceUntilIdle()
        assertEquals(Move.parseUci("e7e5"), vm.state.lastMove)
    }

    @Test
    fun gameOverDialogShowsOnceAndPlayAgainResets() = runTest(dispatcher) {
        // Fool's mate: the computer (Black) mates.
        val vm = GameViewModel(engine = ScriptedEngine("e7e5", "d8h4"))
        vm.move("f2", "f3"); advanceUntilIdle()
        vm.move("g2", "g4"); advanceUntilIdle()
        assertEquals(GameStatus.CHECKMATE, vm.state.status)
        assertTrue(vm.state.showGameOver)

        vm.dismissGameOver()
        assertFalse(vm.state.showGameOver)

        vm.playAgain()
        assertEquals(GameStatus.ONGOING, vm.state.status)
        assertEquals(32, vm.state.pieces.size)
    }

    @Test
    fun oldResultIsNotAnnouncedAgainOnResume() {
        val vm = GameViewModel(saved = SavedGame(Side.WHITE, listOf("f2f3", "e7e5", "g2g4", "d8h4")))
        assertEquals(GameStatus.CHECKMATE, vm.state.status)
        assertFalse(vm.state.showGameOver)
    }

    @Test
    fun materialBalance() = runTest(dispatcher) {
        // 1.e4 d5 2.exd5: White (the player) is a pawn up.
        val vm = GameViewModel(engine = ScriptedEngine("d7d5", "a7a6"))
        vm.move("e2", "e4"); advanceUntilIdle()
        vm.move("e4", "d5"); advanceUntilIdle()
        assertEquals(1, vm.state.materialBalance)
    }
}
