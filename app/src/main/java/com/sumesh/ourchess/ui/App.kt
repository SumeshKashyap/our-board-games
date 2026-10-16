package com.sumesh.ourchess.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumesh.ourchess.engine.Engines
import com.sumesh.ourchess.tutorials.Lessons
import com.sumesh.ourchess.ui.game.GameScreen
import com.sumesh.ourchess.ui.game.GameViewModel
import com.sumesh.ourchess.ui.level.LevelScreen
import com.sumesh.ourchess.ui.loading.LoadingScreen
import com.sumesh.ourchess.ui.menu.MenuScreen
import com.sumesh.ourchess.ui.puzzles.PuzzleScreen
import com.sumesh.ourchess.ui.puzzles.PuzzleViewModel
import com.sumesh.ourchess.ui.tutorials.LessonListScreen
import com.sumesh.ourchess.ui.tutorials.LessonScreen
import com.sumesh.ourchess.ui.tutorials.LessonViewModel
import com.sumesh.ourchess.ui.welcome.WelcomeScreen

@Composable
fun App(app: AppViewModel = viewModel()) {
    BackHandler(enabled = app.canGoBack) { app.back() }

    val profile = app.current
    when (val screen = app.screen) {
        Screen.Loading -> LoadingScreen(isReady = app.isReady, onDone = app::finishLoading)
        Screen.Welcome -> WelcomeScreen(
            profiles = app.profiles,
            onCreate = app::createProfile,
            onChoose = app::choose,
            onDelete = app::deleteProfile,
        )
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
            val vm = viewModel(key = "lesson-${lesson.id}") { LessonViewModel(lesson) }
            LessonScreen(
                vm = vm,
                onComplete = { app.lessonDone(lesson.id) },
                onBack = app::back,
            )
        }
    }
}
