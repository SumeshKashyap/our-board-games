package com.sumesh.ourchess.data

import com.sumesh.ourchess.game.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class) // org.json is part of Android
class SavedGameStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun savesPerPlayerAndLevel() {
        val store = SavedGameStore(tmp.root.resolve("games"))
        assertNull(store.load("asha", 3))

        store.save("asha", 3, SavedGame(Side.BLACK, listOf("e2e4", "c7c5")))
        store.save("asha", 1, SavedGame(Side.WHITE, listOf("d2d4")))
        store.save("ravi", 3, SavedGame(Side.WHITE, emptyList()))

        val reloaded = SavedGameStore(tmp.root.resolve("games"))
        assertEquals(SavedGame(Side.BLACK, listOf("e2e4", "c7c5")), reloaded.load("asha", 3))
        assertEquals(listOf("d2d4"), reloaded.load("asha", 1)?.moves)

        reloaded.deleteAll("asha")
        assertNull(reloaded.load("asha", 3))
        assertNull(reloaded.load("asha", 1))
        assertEquals(SavedGame(Side.WHITE, emptyList()), reloaded.load("ravi", 3))
    }
}
