package com.sumesh.ourchess.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sumesh.ourchess.engine.ChessEngine
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.game.GameScreen
import com.sumesh.ourchess.ui.game.GameViewModel
import com.sumesh.ourchess.ui.theme.OurChessTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * Renders screens at phone and tablet sizes into app/build/outputs/roborazzi/.
 * Run with: ./gradlew recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Deterministic opponent so screenshots are stable. */
    private class ScriptedEngine(vararg moves: String) : ChessEngine {
        private val queue = ArrayDeque(moves.toList())
        override suspend fun bestMove(fen: String): String? = queue.removeFirstOrNull()
    }

    private fun capture(name: String) {
        // Let delayed moves (opponent replies, puzzle setup) and animations finish.
        ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun app(setup: AppViewModel.() -> Unit = {}): AppViewModel {
        val app = AppViewModel(compose.activity.application)
        app.finishLoading()
        app.setup()
        compose.setContent { OurChessTheme { App(app) } }
        return app
    }

    private fun AppViewModel.withPlayers() {
        createProfile("Ravi")
        chooseLevel(2)
        switchPlayer()
        createProfile("Asha")
        chooseLevel(3)
    }

    @Test fun welcomeFirstTime() { app(); capture("01_welcome_first_time") }

    @Test fun welcomeChoosePlayer() { app { withPlayers(); switchPlayer() }; capture("02_welcome_players") }

    @Test fun level() { app { createProfile("Asha") }; capture("03_level") }

    @Test fun menu() { app { withPlayers() }; capture("04_menu") }

    @Test fun puzzle() { app { withPlayers(); open(Screen.Puzzles) }; capture("05_puzzle") }

    @Test fun lessonList() { app { withPlayers(); lessonDone("castling"); open(Screen.Lessons) }; capture("06_lessons") }

    @Test fun lesson() { app { withPlayers(); open(Screen.Lesson("fork")) }; capture("07_lesson_fork") }

    @Test fun photos() { app { withPlayers(); open(Screen.Photos) }; capture("12_photos") }

    @Test @Config(qualifiers = TABLET)
    fun menuTablet() { app { withPlayers() }; capture("08_menu_tablet") }

    @Test @Config(qualifiers = TABLET)
    fun puzzleTablet() { app { withPlayers(); open(Screen.Puzzles) }; capture("09_puzzle_tablet") }

    /** A few moves in, with a piece selected so legal-move dots and a capture ring are visible. */
    private fun midGame(): GameViewModel {
        val vm = GameViewModel(ScriptedEngine("d7d5", "b8c6"))
        compose.setContent { OurChessTheme { GameScreen(vm, level = 3, onBack = {}) } }
        vm.newGame(Side.WHITE)
        vm.onSquareTapped(Square.parse("e2")); vm.onSquareTapped(Square.parse("e4"))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        vm.onSquareTapped(Square.parse("g1")); vm.onSquareTapped(Square.parse("f3"))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        vm.onSquareTapped(Square.parse("e4"))
        return vm
    }

    @Test fun gamePhone() { midGame(); capture("10_game_phone") }

    @Test @Config(qualifiers = TABLET)
    fun gameTablet() { midGame(); capture("11_game_tablet") }
}

private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val TABLET = "w1280dp-h800dp-land-mdpi"
