package com.sumesh.ourchess.ludo

import com.sumesh.ourchess.ludo.LudoColor.GREEN
import com.sumesh.ourchess.ludo.LudoColor.RED
import com.sumesh.ourchess.ludo.LudoColor.YELLOW
import com.sumesh.ourchess.ludo.LudoGeometry.HOME
import com.sumesh.ourchess.ludo.LudoGeometry.YARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class LudoTest {

    private fun state(red: List<Int>, yellow: List<Int> = List(4) { YARD }, dice: Int? = null) =
        LudoState(listOf(RED, YELLOW), mapOf(RED to red, YELLOW to yellow), dice = dice)

    @Test
    fun geometryIsAConnectedLoop() {
        val track = LudoGeometry.TRACK
        assertEquals(52, track.size)
        assertEquals(52, track.toSet().size)
        // Each step moves to a neighbouring cell (including the corner diagonals of the cross).
        for (i in track.indices) {
            val a = track[i]; val b = track[(i + 1) % track.size]
            assertTrue("gap at $i: $a -> $b", abs(a.col - b.col) <= 1 && abs(a.row - b.row) <= 1)
        }
        // Each colour's home column starts next to its last track square.
        for (color in LudoColor.entries) {
            val last = LudoGeometry.cellOf(color, LudoGeometry.LAST_TRACK)!!
            val first = LudoGeometry.homeColumn(color).first()
            assertEquals("$color", 1, abs(last.col - first.col) + abs(last.row - first.row))
        }
        assertEquals(8, LudoGeometry.SAFE.size)
    }

    @Test
    fun needSixToLeaveTheYard() {
        val (afterFive, rolled5) = LudoRules.roll(LudoState.newGame(listOf(RED, YELLOW)), 5)
        assertFalse(rolled5.canMove)
        assertEquals(YELLOW, afterFive.currentColor) // turn passed

        val (afterSix, rolled6) = LudoRules.roll(LudoState.newGame(listOf(RED, YELLOW)), 6)
        assertTrue(rolled6.canMove)
        val (moved, event) = LudoRules.move(afterSix, 0)
        assertEquals(0, moved.tokens.getValue(RED)[0])
        assertTrue(event.extraTurn) // a 6 earns another roll
        assertEquals(RED, moved.currentColor)
    }

    @Test
    fun exactRollNeededToReachHome() {
        val s = state(red = listOf(53, YARD, YARD, YARD))
        assertTrue(LudoRules.roll(s, 4).second.canMove.not()) // 53 + 4 = 57 overshoots
        val (rolled, _) = LudoRules.roll(s, 3)
        val (moved, event) = LudoRules.move(rolled, 0)
        assertEquals(HOME, moved.tokens.getValue(RED)[0])
        assertTrue(event.extraTurn) // reaching home earns another roll
    }

    @Test
    fun landingOnOpponentCapturesIt() {
        // Red token at progress 5 (track 5); Yellow token at its progress 31 = track (26 + 31) % 52 = 5.
        val s = state(red = listOf(2, YARD, YARD, YARD), yellow = listOf(31, YARD, YARD, YARD))
        val (rolled, _) = LudoRules.roll(s, 3)
        val (moved, event) = LudoRules.move(rolled, 0)
        assertEquals(listOf(YELLOW to 0), event.captured)
        assertEquals(YARD, moved.tokens.getValue(YELLOW)[0])
        assertTrue(event.extraTurn)
    }

    @Test
    fun safeSquaresProtect() {
        // Track 8 is a star. Yellow at progress 34 = track 8.
        val s = state(red = listOf(5, YARD, YARD, YARD), yellow = listOf(34, YARD, YARD, YARD))
        val (rolled, _) = LudoRules.roll(s, 3)
        val (moved, event) = LudoRules.move(rolled, 0)
        assertTrue(event.captured.isEmpty())
        assertEquals(34, moved.tokens.getValue(YELLOW)[0])
        assertFalse(event.extraTurn)
        assertEquals(YELLOW, moved.currentColor)
    }

    @Test
    fun threeSixesLoseTheTurn() {
        var s = state(red = listOf(0, YARD, YARD, YARD))
        repeat(2) {
            s = LudoRules.roll(s, 6).first
            s = LudoRules.move(s, 0).first
            assertEquals(RED, s.currentColor)
        }
        val (after, event) = LudoRules.roll(s, 6)
        assertTrue(event.lostTurnOnThreeSixes)
        assertEquals(YELLOW, after.currentColor)
        assertEquals(12, after.tokens.getValue(RED)[0]) // the earlier moves stand
    }

    @Test
    fun lastTokenHomeWins() {
        val s = state(red = listOf(HOME, HOME, HOME, 55))
        val (rolled, _) = LudoRules.roll(s, 1)
        val (moved, event) = LudoRules.move(rolled, 3)
        assertEquals(RED, moved.winner)
        assertFalse(event.extraTurn)
    }

    @Test
    fun twoPlayersSitOpposite() {
        assertEquals(listOf(RED, YELLOW), LudoColor.forPlayers(2))
        assertEquals(listOf(RED, GREEN, YELLOW), LudoColor.forPlayers(3))
        assertEquals(4, LudoColor.forPlayers(4).size)
    }

    @Test
    fun aiPrefersCapturingAndGettingHome() {
        val ai = LudoAi(Random(3))
        // Token 0 can capture (lands on Yellow at track 5); token 1 would just walk.
        val capture = state(red = listOf(2, 20, YARD, YARD), yellow = listOf(31, YARD, YARD, YARD), dice = 3)
        assertEquals(0, ai.choose(capture, level = 5))
        val home = state(red = listOf(53, 10, YARD, YARD), dice = 3)
        assertEquals(0, ai.choose(home, level = 5))
        assertNull(ai.choose(state(red = List(4) { YARD }, dice = 3), level = 5))
    }

    @Test
    fun computerGamesAlwaysFinish() {
        repeat(5) { seed ->
            val rnd = Random(seed)
            val ai = LudoAi(rnd)
            var s = LudoState.newGame(LudoColor.forPlayers(4))
            var turns = 0
            while (s.winner == null && turns < 5_000) {
                s = LudoRules.roll(s, rnd.nextInt(1, 7)).first
                if (s.dice != null) s = LudoRules.move(s, ai.choose(s, level = 3)!!).first
                turns++
            }
            assertTrue("game $seed didn't finish", s.winner != null)
        }
    }
}
