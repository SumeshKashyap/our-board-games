package com.sumesh.ourchess.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.engine.Engines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sumesh.ourchess.data.Profile
import com.sumesh.ourchess.data.ProfileRepository
import com.sumesh.ourchess.data.SavedGame
import com.sumesh.ourchess.data.SavedGameStore
import com.sumesh.ourchess.puzzles.Puzzle
import com.sumesh.ourchess.puzzles.PuzzleRepository
import java.io.File

sealed interface Screen {
    data object Loading : Screen
    data object Welcome : Screen
    data object Level : Screen
    data object Menu : Screen
    data object Game : Screen
    data object Puzzles : Screen
    data object Lessons : Screen
    data class Lesson(val id: String) : Screen
}

/** App-wide state: profiles, the current player, and which screen is showing. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProfileRepository(File(app.filesDir, "profiles.json"))
    private val savedGames = SavedGameStore(File(app.filesDir, "games"))
    private val allPuzzles: List<Puzzle> by lazy { PuzzleRepository.load(app.assets) }

    /** Most recently used first. */
    var profiles by mutableStateOf(loadProfiles())
        private set
    var current by mutableStateOf<Profile?>(null)
        private set

    private val backStack = mutableStateListOf<Screen>(Screen.Loading)

    /** True once slow start-up work (engine start, puzzle loading) has finished. */
    var isReady by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                allPuzzles.size
                runCatching { Engines.warmUp() }
            }
            isReady = true
        }
    }

    fun finishLoading() {
        if (screen != Screen.Loading) return
        backStack.clear()
        backStack.add(Screen.Welcome)
    }
    val screen: Screen get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1

    fun open(screen: Screen) {
        backStack.add(screen)
    }

    fun back() {
        if (canGoBack) backStack.removeAt(backStack.lastIndex)
    }

    fun createProfile(name: String) {
        val profile = repo.create(name)
        profiles = loadProfiles()
        choose(profile)
    }

    fun choose(profile: Profile) {
        current = save(profile.copy(lastUsed = System.currentTimeMillis()))
        open(Screen.Level)
    }

    fun deleteProfile(profile: Profile) {
        repo.delete(profile.id)
        savedGames.deleteAll(profile.id)
        profiles = loadProfiles()
    }

    /** Back to the welcome screen to pick another player. */
    fun switchPlayer() {
        backStack.clear()
        backStack.add(Screen.Welcome)
        current = null
    }

    fun chooseLevel(level: Int) {
        val profile = current ?: return
        current = save(profile.copy(level = level))
        // Changing the level from the menu returns to the menu; first time through, continue to it.
        if (backStack.getOrNull(backStack.lastIndex - 1) == Screen.Menu) back() else open(Screen.Menu)
    }

    fun savedGame(profile: Profile): SavedGame? = savedGames.load(profile.id, profile.level)

    fun saveGame(profile: Profile, level: Int, game: SavedGame) = savedGames.save(profile.id, level, game)

    fun puzzlesForLevel(level: Int): List<Puzzle> = allPuzzles.filter { it.level == level }

    fun puzzleSolved() {
        val profile = current ?: return
        current = save(profile.copy(puzzlesSolved = profile.puzzlesSolved + 1))
    }

    fun setNextPuzzle(level: Int, index: Int) {
        val profile = current ?: return
        current = save(profile.copy(nextPuzzle = profile.nextPuzzle + (level to index)))
    }

    fun lessonDone(id: String) {
        val profile = current ?: return
        current = save(profile.copy(lessonsDone = profile.lessonsDone + id))
    }

    private fun save(profile: Profile): Profile {
        repo.update(profile)
        profiles = loadProfiles()
        return profile
    }

    private fun loadProfiles() = repo.load().sortedByDescending { it.lastUsed }
}
