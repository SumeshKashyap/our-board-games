package com.sumesh.ourchess.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import com.sumesh.ourchess.ui.common.Magnifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumesh.ourchess.data.PlayerSettings
import com.sumesh.ourchess.engine.Engines
import com.sumesh.ourchess.tutorials.Lessons
import com.sumesh.ourchess.ui.game.GameScreen
import com.sumesh.ourchess.ui.game.GameViewModel
import com.sumesh.ourchess.ui.level.LevelScreen
import com.sumesh.ourchess.ui.loading.LoadingOverlay
import com.sumesh.ourchess.ui.loading.PhotoBackdrop
import com.sumesh.ourchess.ui.menu.MenuScreen
import com.sumesh.ourchess.ui.photos.PhotosScreen
import com.sumesh.ourchess.ui.puzzles.PuzzleScreen
import com.sumesh.ourchess.ui.puzzles.PuzzleViewModel
import com.sumesh.ourchess.ui.settings.SettingsScreen
import com.sumesh.ourchess.ui.theme.LocalPlayerSettings
import com.sumesh.ourchess.ui.tutorials.LessonListScreen
import com.sumesh.ourchess.ui.tutorials.LessonScreen
import com.sumesh.ourchess.ui.tutorials.LessonViewModel
import com.sumesh.ourchess.ui.welcome.WelcomeScreen

@Composable
fun App(app: AppViewModel = viewModel()) {
    BackHandler(enabled = app.canGoBack) { app.back() }
    val base = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        // Every screen follows the current player's settings (board colours, large print, …),
        // and the magnifier's zoom scales everything (a larger density re-flows layouts rather than cropping).
        CompositionLocalProvider(
            LocalPlayerSettings provides (app.current?.settings ?: PlayerSettings()),
            LocalDensity provides Density(base.density * app.zoom, base.fontScale),
        ) {
            CurrentScreen(app)
        }
        // Outside the zoom, so the magnifier itself always stays the same size and place.
        if (app.screen != Screen.Loading) {
            Magnifier(
                zoom = app.zoom,
                onZoom = app::setZoom,
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(12.dp),
            )
        }
    }
}

@Composable
private fun CurrentScreen(app: AppViewModel) {
    val profile = app.current
    // Read fresh each time, so a change in Settings applies to games already in progress.
    val settings = { app.current?.settings ?: PlayerSettings() }

    when (val screen = app.screen) {
        // The start-up photos stay behind the welcome screen, fading from bright to a soft background.
        Screen.Loading, Screen.Welcome -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val loading = screen == Screen.Loading
            PhotoBackdrop(app.startupPhotos, dimmed = !loading)
            Crossfade(loading, animationSpec = tween(1500), label = "start") { showLoading ->
                if (showLoading) {
                    LoadingOverlay(hasPhotos = app.startupPhotos.isNotEmpty(), isReady = app.isReady, onDone = app::finishLoading)
                } else {
                    WelcomeScreen(
                        profiles = app.profiles,
                        onCreate = app::createProfile,
                        onChoose = app::choose,
                        onDelete = app::deleteProfile,
                    )
                }
            }
        }
        Screen.Level -> LevelScreen(
            name = profile?.name.orEmpty(),
            current = profile?.level ?: 1,
            onChoose = app::chooseLevel,
            onBack = app::back,
        )
        Screen.Menu -> MenuScreen(
            profile = profile ?: return,
            hasGameInProgress = app.savedGame(profile).let { it != null && it.moves.isNotEmpty() },
            onPlay = { app.open(Screen.Game) },
            onPuzzles = { app.open(Screen.Puzzles) },
            onLessons = { app.open(Screen.Lessons) },
            photoCount = app.photosVersion.let { app.photosOf(profile).size },
            onPhotos = { app.open(Screen.Photos) },
            onSettings = { app.open(Screen.Settings) },
            onChangeLevel = { app.open(Screen.Level) },
            onSwitchPlayer = app::switchPlayer,
        )
        Screen.Game -> {
            val p = profile ?: return
            // One game per player and level, so leaving to the menu and coming back resumes it.
            val vm = viewModel(key = "game-${p.id}-${p.level}") {
                GameViewModel(
                    engine = Engines.forLevel(p.level),
                    hintEngine = Engines.forHints(),
                    saved = app.savedGame(p),
                    onSave = { game -> app.saveGame(p, p.level, game) },
                    settings = settings,
                )
            }
            GameScreen(vm = vm, level = p.level, onBack = app::back)
        }
        Screen.Puzzles -> {
            val p = profile ?: return
            val vm = viewModel(key = "puzzles-${p.id}-${p.level}") {
                PuzzleViewModel(
                    puzzles = app.puzzlesForLevel(p.level),
                    startIndex = p.nextPuzzle[p.level] ?: 0,
                    onSolved = app::puzzleSolved,
                    onAdvance = { index -> app.setNextPuzzle(p.level, index) },
                    autoQueen = { settings().autoQueen },
                )
            }
            PuzzleScreen(vm = vm, level = p.level, solvedCount = p.puzzlesSolved, onBack = app::back)
        }
        Screen.Photos -> PhotosScreen(
            photos = app.photosVersion.let { profile?.let(app::photosOf).orEmpty() },
            repository = app.photos,
            adding = app.addingPhotos,
            onAdd = app::addPhotos,
            onRemove = app::removePhoto,
            onBack = app::back,
        )
        Screen.Settings -> SettingsScreen(
            name = profile?.name.orEmpty(),
            settings = settings(),
            onChange = app::updateSettings,
            onBack = app::back,
        )
        Screen.Lessons -> LessonListScreen(
            done = profile?.lessonsDone.orEmpty(),
            onOpen = { app.open(Screen.Lesson(it.id)) },
            onBack = app::back,
        )
        is Screen.Lesson -> {
            val lesson = Lessons.byId(screen.id)
            val vm = viewModel(key = "lesson-${lesson.id}") { LessonViewModel(lesson) { settings().autoQueen } }
            LessonScreen(
                vm = vm,
                onComplete = { app.lessonDone(lesson.id) },
                onBack = app::back,
            )
        }
    }
}
