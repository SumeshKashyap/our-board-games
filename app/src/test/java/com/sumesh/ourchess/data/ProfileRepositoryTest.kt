package com.sumesh.ourchess.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class) // org.json is part of Android
class ProfileRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun createUpdateDeleteSurvivesReload() {
        val file = tmp.root.resolve("profiles.json")
        val repo = ProfileRepository(file)
        assertTrue(repo.load().isEmpty())

        val asha = repo.create("  Asha ")
        val ravi = repo.create("Ravi")
        val settings = PlayerSettings(boardTheme = BoardTheme.HIGH_CONTRAST, largePrint = true, speakMoves = true)
        repo.update(asha.copy(level = 4, nextPuzzle = mapOf(4 to 7), puzzlesSolved = 3, lessonsDone = setOf("fork"), settings = settings))

        val reloaded = ProfileRepository(file).load()
        assertEquals(listOf("Asha", "Ravi"), reloaded.map { it.name })
        val a = reloaded.first { it.id == asha.id }
        assertEquals(4, a.level)
        assertEquals(mapOf(4 to 7), a.nextPuzzle)
        assertEquals(3, a.puzzlesSolved)
        assertEquals(setOf("fork"), a.lessonsDone)
        assertEquals(settings, a.settings)
        assertEquals(PlayerSettings(), reloaded.first { it.id == ravi.id }.settings)

        repo.delete(ravi.id)
        assertEquals(listOf(asha.id), ProfileRepository(file).load().map { it.id })
    }
}
