package com.sumesh.ourchess.ludo

import com.sumesh.ourchess.ui.ludo.LudoGameViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LudoGameTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private val seats = listOf(
        LudoSeat(LudoColor.RED, "Asha", "asha-id", computer = false),
        LudoSeat(LudoColor.YELLOW, "Computer", null, computer = true),
    )

    @Test
    fun saveRoundTrip() {
        val state = LudoState(
            seats.map { it.color },
            mapOf(LudoColor.RED to listOf(-1, 5, 56, 52), LudoColor.YELLOW to listOf(0, -1, -1, 30)),
            current = 1, dice = 4, sixesInRow = 1,
        )
        val saved = LudoSaved(seats, state)
        assertEquals(saved, LudoSaved.decode(saved.encode()))
    }

    @Test
    fun computersPlayAFullGameAndReportTheWinner() = runTest(dispatcher) {
        val bots = LudoColor.forPlayers(3).map { LudoSeat(it, "Computer", null, computer = true) }
        var winner: LudoSeat? = null
        var saves = 0
        val vm = LudoGameViewModel(bots, level = 3, onSave = { saves++ }, onFinished = { w, _ -> winner = w }, random = Random(5))
        advanceUntilIdle()
        assertNotNull(winner)
        assertEquals(winner!!.color, vm.state.game.winner)
        assertTrue(saves > 50)
        assertTrue(vm.state.showGameOver)
    }

    @Test
    fun familyMemberRollsAndTaps() = runTest(dispatcher) {
        val vm = LudoGameViewModel(seats, level = 3, random = Random(1))
        advanceUntilIdle()
        assertTrue(vm.state.canRoll)
        assertEquals(LudoColor.RED, vm.state.currentSeat.color)
        vm.roll()
        assertFalse(vm.state.canRoll) // rolling
        advanceUntilIdle()
        // Either the turn passed to the computer (and came back), or a token can now be chosen.
        assertTrue(vm.state.canRoll || vm.state.movable.isNotEmpty() || vm.state.game.current == 0)
    }
}
