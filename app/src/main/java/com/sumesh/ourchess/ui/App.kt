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
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersLessons
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.data.GameKind
import com.sumesh.ourchess.data.Outcome
import com.sumesh.ourchess.data.Stats
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.checkers.CheckersGameScreen
import com.sumesh.ourchess.ui.checkers.CheckersGameViewModel
import com.sumesh.ourchess.ui.checkers.CheckersLessonListScreen
import com.sumesh.ourchess.ui.checkers.CheckersLessonScreen
import com.sumesh.ourchess.ui.checkers.CheckersLessonViewModel
import com.sumesh.ourchess.ui.checkers.CheckersPuzzleScreen
import com.sumesh.ourchess.ui.checkers.CheckersPuzzleViewModel
import com.sumesh.ourchess.ui.checkers.PieceDisc
import com.sumesh.ourchess.ui.common.PieceIcon
import com.sumesh.ourchess.ui.home.GameMenuItem
import com.sumesh.ourchess.ui.home.GameMenuScreen
import com.sumesh.ourchess.ui.home.HomeScreen
import com.sumesh.ourchess.ui.ludo.DiceFace
import com.sumesh.ourchess.ui.ludo.LudoGameScreen
import com.sumesh.ourchess.ui.ludo.LudoGameViewModel
import com.sumesh.ourchess.ui.ludo.LudoSetupScreen
import com.sumesh.ourchess.ui.ludo.LudoTutorialScreen
import com.sumesh.ourchess.ui.ludo.LudoTutorialViewModel
import com.sumesh.ourchess.ui.stats.StatsScreen
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
        Screen.Menu -> HomeScreen(
            profile = profile ?: return,
            photoCount = app.photosVersion.let { app.photosOf(profile).size },
            onChess = { app.open(Screen.ChessMenu) },
            onCheckers = { app.open(Screen.CheckersMenu) },
            onLudo = { app.open(Screen.LudoMenu) },
            onStats = { app.open(Screen.Stats) },
            onPhotos = { app.open(Screen.Photos) },
            onSettings = { app.open(Screen.Settings) },
            onChangeLevel = { app.open(Screen.Level) },
            onSwitchPlayer = app::switchPlayer,
        )
        Screen.Stats -> StatsScreen(profile?.name.orEmpty(), profile?.stats ?: Stats(), onBack = app::back)

        // --- Chess ---
        Screen.ChessMenu -> {
            val p = profile ?: return
            val inProgress = app.savedGame(p).let { it != null && it.moves.isNotEmpty() }
            GameMenuScreen(
                "Chess",
                icon = { PieceIcon(Side.WHITE, PieceType.KING) },
                items = listOf(
                    GameMenuItem("Play against computer", if (inProgress) "Continue your game" else "A full game at your level") { app.open(Screen.Game) },
                    GameMenuItem("Puzzles", if (p.puzzlesSolved > 0) "${p.puzzlesSolved} solved so far" else "Find the winning move") { app.open(Screen.Puzzles) },
                    GameMenuItem("Learn moves", "${p.lessonsDone.count { ':' !in it }} of ${Lessons.all.size} lessons done") { app.open(Screen.Lessons) },
                ),
                onBack = app::back,
            )
        }
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
                    onResult = { app.recordResult(p.id, GameKind.CHESS, p.level, it) },
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
        Screen.Lessons -> LessonListScreen(
            done = profile?.lessonsDone.orEmpty(),
            onOpen = { app.open(Screen.Lesson(it.id)) },
            onBack = app::back,
        )
        is Screen.Lesson -> {
            val lesson = Lessons.byId(screen.id)
            val vm = viewModel(key = "lesson-${lesson.id}") { LessonViewModel(lesson) { settings().autoQueen } }
            LessonScreen(vm = vm, onComplete = { app.lessonDone(lesson.id) }, onBack = app::back)
        }

        // --- Checkers ---
        Screen.CheckersMenu -> {
            val p = profile ?: return
            val inProgress = app.savedCheckers(p)?.substringAfter('|')?.isNotBlank() == true
            val solved = app.progress("checkers.solved")
            GameMenuScreen(
                "Checkers",
                icon = { PieceDisc(CheckersPiece(CheckersColor.BLACK, king = true), 56.dp) },
                items = listOf(
                    GameMenuItem("Play against computer", if (inProgress) "Continue your game" else "A full game at your level") { app.open(Screen.CheckersGame) },
                    GameMenuItem("Puzzles", if (solved > 0) "$solved solved so far" else "Find the winning move") { app.open(Screen.CheckersPuzzles) },
                    GameMenuItem(
                        "Learn checkers",
                        "${CheckersLessons.all.count { "checkers:${it.id}" in p.lessonsDone }} of ${CheckersLessons.all.size} lessons done",
                    ) { app.open(Screen.CheckersLessons) },
                ),
                onBack = app::back,
            )
        }
        Screen.CheckersGame -> {
            val p = profile ?: return
            val vm = viewModel(key = "checkers-${p.id}-${p.level}") {
                CheckersGameViewModel(
                    level = p.level,
                    saved = app.savedCheckers(p),
                    onSave = { app.saveCheckers(p, p.level, it) },
                    onResult = { app.recordResult(p.id, GameKind.CHECKERS, p.level, it) },
                    settings = settings,
                )
            }
            CheckersGameScreen(vm, p.level, onBack = app::back)
        }
        Screen.CheckersPuzzles -> {
            val p = profile ?: return
            val key = "checkers.puzzle.L${p.level}"
            val vm = viewModel(key = "checkers-puzzles-${p.id}-${p.level}") {
                CheckersPuzzleViewModel(
                    puzzles = app.checkersPuzzlesForLevel(p.level),
                    startIndex = app.progress(key),
                    onSolved = { app.setProgress("checkers.solved", app.progress("checkers.solved") + 1) },
                    onAdvance = { app.setProgress(key, it) },
                )
            }
            CheckersPuzzleScreen(vm, p.level, app.progress("checkers.solved"), onBack = app::back)
        }
        Screen.CheckersLessons -> CheckersLessonListScreen(
            done = profile?.lessonsDone.orEmpty().filter { it.startsWith("checkers:") }.map { it.removePrefix("checkers:") }.toSet(),
            onOpen = { app.open(Screen.CheckersLesson(it.id)) },
            onBack = app::back,
        )
        is Screen.CheckersLesson -> {
            val lesson = CheckersLessons.byId(screen.id)
            val vm = viewModel(key = "checkers-lesson-${lesson.id}") { CheckersLessonViewModel(lesson) }
            CheckersLessonScreen(vm, onComplete = { app.lessonDone("checkers:${lesson.id}") }, onBack = app::back)
        }

        // --- Ludo ---
        Screen.LudoMenu -> {
            val p = profile ?: return
            val saved = app.savedLudo(p)
            val items = buildList {
                if (saved != null && saved.state.winner == null) {
                    add(GameMenuItem("Continue game", saved.seats.joinToString(", ") { if (it.computer) "Computer" else it.name }) { app.open(Screen.LudoGame) })
                }
                add(GameMenuItem("New game", "Choose 2 to 4 players: family or computer") { app.open(Screen.LudoSetup) })
                add(GameMenuItem("How to play", if ("ludo:tutorial" in p.lessonsDone) "Done ✓ — play it again any time" else "A short hands-on guide") { app.open(Screen.LudoTutorial) })
            }
            GameMenuScreen("Ludo", icon = { DiceFace(5, 52.dp) }, items = items, onBack = app::back)
        }
        Screen.LudoSetup -> LudoSetupScreen(
            host = profile ?: return,
            profiles = app.profiles,
            onStart = app::startLudo,
            onBack = app::back,
        )
        Screen.LudoGame -> {
            val p = profile ?: return
            val saved = app.savedLudo(p) ?: return
            val vm = viewModel(key = "ludo-${p.id}-${app.ludoGameId}") {
                LudoGameViewModel(
                    seats = saved.seats,
                    level = p.level,
                    saved = saved,
                    onSave = { app.saveLudo(p, it) },
                    onFinished = { winner, seats ->
                        for (seat in seats) {
                            val id = seat.profileId ?: continue
                            app.recordResult(id, GameKind.LUDO, p.level, if (seat == winner) Outcome.WON else Outcome.LOST)
                        }
                    },
                )
            }
            LudoGameScreen(vm, onBack = app::back, onNewGame = { app.back(); app.open(Screen.LudoSetup) })
        }
        Screen.LudoTutorial -> {
            val vm = viewModel(key = "ludo-tutorial") { LudoTutorialViewModel() }
            LudoTutorialScreen(vm, onComplete = { app.lessonDone("ludo:tutorial") }, onBack = app::back)
        }

        // --- Player ---
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
    }
}
