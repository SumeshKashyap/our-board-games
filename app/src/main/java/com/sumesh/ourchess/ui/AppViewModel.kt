package com.sumesh.ourchess.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.data.PhotoRepository
import com.sumesh.ourchess.data.PlayerSettings
import com.sumesh.ourchess.data.Profile
import com.sumesh.ourchess.data.ProfileRepository
import com.sumesh.ourchess.data.SavedGame
import com.sumesh.ourchess.data.SavedGameStore
import com.sumesh.ourchess.data.pickRandom
import com.sumesh.ourchess.engine.Engines
import com.sumesh.ourchess.puzzles.Puzzle
import com.sumesh.ourchess.puzzles.PuzzleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Screen {
    data object Loading : Screen
    data object Welcome : Screen
    data object Level : Screen
    data object Menu : Screen
    data object Game : Screen
    data object Puzzles : Screen
    data object Lessons : Screen
    data object Photos : Screen
    data object Settings : Screen
    data class Lesson(val id: String) : Screen
}

/** App-wide state: profiles, the current player, and which screen is showing. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProfileRepository(File(app.filesDir, "profiles.json"))
    private val savedGames = SavedGameStore(File(app.filesDir, "games"))
    private val allPuzzles: List<Puzzle> by lazy { PuzzleRepository.load(app.assets) }
    val photos = PhotoRepository(app)

    /** Most recently used first. */
    var profiles by mutableStateOf(loadProfiles())
        private set
    var current by mutableStateOf<Profile?>(null)
        private set

    private val backStack = mutableStateListOf<Screen>(Screen.Loading)

    /** True once slow start-up work (engine start, puzzle loading) has finished. */
    var isReady by mutableStateOf(false)
        private set

    /** Three photos picked at random for this launch, from the family's and every player's own. */
    var startupPhotos by mutableStateOf<List<ImageBitmap>>(emptyList())
        private set

    /** Bumped whenever a player's photos change, so screens re-read them. */
    var photosVersion by mutableIntStateOf(0)
        private set
    var addingPhotos by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val chosen = async(Dispatchers.IO) {
                pickRandom(photos.all(profiles.map { it.id }), STARTUP_PHOTOS)
                    .mapNotNull { photos.load(it, maxSize = 1400)?.asImageBitmap() }
            }
            startupPhotos = chosen.await()
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
        photos.removeAll(profile.id)
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

    /** Zoom chosen before a player is picked (not saved); otherwise the most recent player's. */
    private var guestZoom by mutableStateOf<Float?>(null)

    /** How big everything is drawn: the current player's choice, or the most recent player's on the welcome screen. */
    val zoom: Float
        get() = current?.settings?.zoom ?: guestZoom ?: profiles.firstOrNull()?.settings?.zoom ?: 1f

    fun setZoom(zoom: Float) {
        val profile = current
        if (profile == null) guestZoom = zoom else updateSettings(profile.settings.copy(zoom = zoom))
    }

    fun updateSettings(settings: PlayerSettings) {
        val profile = current ?: return
        current = save(profile.copy(settings = settings))
    }

    fun photosOf(profile: Profile): List<File> = photos.forProfile(profile.id)

    fun addPhotos(uris: List<Uri>) {
        val profile = current ?: return
        addingPhotos = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) { uris.forEach { photos.add(profile.id, it) } }
            addingPhotos = false
            photosVersion++
        }
    }

    fun removePhoto(file: File) {
        photos.remove(file)
        photosVersion++
    }

    private fun save(profile: Profile): Profile {
        repo.update(profile)
        profiles = loadProfiles()
        return profile
    }

    private fun loadProfiles() = repo.load().sortedByDescending { it.lastUsed }

    private companion object {
        const val STARTUP_PHOTOS = 3
    }
}
