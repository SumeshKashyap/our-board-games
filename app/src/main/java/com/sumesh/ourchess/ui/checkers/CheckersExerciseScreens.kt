package com.sumesh.ourchess.ui.checkers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersLesson
import com.sumesh.ourchess.checkers.CheckersLessons
import com.sumesh.ourchess.checkers.CheckersPosition
import com.sumesh.ourchess.checkers.CheckersPuzzle
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ButtonGroup
import com.sumesh.ourchess.ui.common.ChoiceCard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.common.StatusBanner
import com.sumesh.ourchess.ui.common.StatusColors
import com.sumesh.ourchess.ui.exercise.ExercisePhase
import com.sumesh.ourchess.ui.feedback.MoveFeedback

/** The board for a checkers exercise, with move feedback. */
@Composable
private fun CheckersExerciseBoard(session: CheckersExerciseSession, modifier: Modifier) {
    val s = session.state
    CheckersBoard(
        pieces = s.pieces,
        flipped = s.userColor == CheckersColor.BLACK,
        selected = s.selected,
        partialPath = s.partialPath,
        targets = s.targets,
        lastPath = s.lastPath,
        animation = s.animation,
        targetsAreHint = s.hintShown,
        onSquareTap = session::onSquareTapped,
        modifier = modifier,
    )
    MoveFeedback(s.lastMoveEvent, opponentName = "Opponent")
}

// --- Puzzles ---------------------------------------------------------------------------------

class CheckersPuzzleViewModel(
    private val puzzles: List<CheckersPuzzle>,
    startIndex: Int,
    private val onSolved: () -> Unit,
    private val onAdvance: (Int) -> Unit,
) : ViewModel() {
    var index by mutableIntStateOf(startIndex.coerceIn(0, (puzzles.size - 1).coerceAtLeast(0)))
        private set
    val count: Int get() = puzzles.size
    val puzzle: CheckersPuzzle get() = puzzles[index]
    var session by mutableStateOf(newSession())
        private set

    fun next() {
        session.cancel()
        index = (index + 1) % puzzles.size
        onAdvance(index)
        session = newSession()
    }

    fun retry() {
        session.cancel()
        session = newSession()
    }

    private fun newSession(): CheckersExerciseSession {
        val p = puzzle
        return CheckersExerciseSession(viewModelScope, p.position, p.solution) { unaided ->
            if (unaided) onSolved()
            onAdvance((puzzles.indexOf(p) + 1) % puzzles.size)
        }
    }
}

@Composable
fun CheckersPuzzleScreen(vm: CheckersPuzzleViewModel, level: Int, solvedCount: Int, onBack: () -> Unit) {
    val session = vm.session
    val state = session.state
    val color = if (state.userColor == CheckersColor.WHITE) "White" else "Black"
    BoardScreenLayout(
        header = { ScreenHeader("Checkers puzzles · ${Levels.name(level)}", onBack = onBack, backLabel = "Back") },
        info = {
            val (text, bg) = when (state.phase) {
                ExercisePhase.YOUR_TURN -> "You play $color:\n${vm.puzzle.goal}" to StatusColors.Good
                ExercisePhase.OPPONENT_REPLY, ExercisePhase.OPPONENT_SETUP -> "Good move! Opponent replies…" to StatusColors.Good
                ExercisePhase.WRONG_MOVE -> "Not quite — try again" to StatusColors.Alert
                ExercisePhase.SOLVED -> "Well done — solved!" to StatusColors.Good
                ExercisePhase.SHOWING_SOLUTION, ExercisePhase.SOLUTION_SHOWN -> "Here is the solution" to StatusColors.Neutral
            }
            StatusBanner(text, bg)
            Text("Puzzle ${vm.index + 1} of ${vm.count}  ·  Solved: $solvedCount", style = MaterialTheme.typography.bodyMedium)
        },
        board = { modifier -> CheckersExerciseBoard(session, modifier) },
        actions = { stacked ->
            ButtonGroup(stacked) { m ->
                if (state.isFinished) {
                    BigButton("Try again", vm::retry, m, icon = Icons.Filled.Refresh)
                    BigButton("Next puzzle", vm::next, m, icon = Icons.AutoMirrored.Filled.ArrowForward)
                } else {
                    val active = state.phase == ExercisePhase.YOUR_TURN
                    BigButton("Hint", session::hint, m, enabled = active)
                    BigButton("Show answer", session::showSolution, m, enabled = active || state.phase == ExercisePhase.WRONG_MOVE)
                }
            }
        },
    )
}

// --- Lessons ---------------------------------------------------------------------------------

class CheckersLessonViewModel(val lesson: CheckersLesson) : ViewModel() {
    var stepIndex by mutableIntStateOf(0)
        private set
    val isLastStep: Boolean get() = stepIndex == lesson.steps.lastIndex
    var session by mutableStateOf(newSession())
        private set

    fun nextStep() {
        if (isLastStep) return
        session.cancel()
        stepIndex++
        session = newSession()
    }

    fun restartStep() {
        session.cancel()
        session = newSession()
    }

    private fun newSession() = lesson.steps[stepIndex].let { step ->
        CheckersExerciseSession(viewModelScope, CheckersPosition.fromKey(step.key.replace(';', ',')), step.solution)
    }
}

@Composable
fun CheckersLessonListScreen(done: Set<String>, onOpen: (CheckersLesson) -> Unit, onBack: () -> Unit) {
    MenuPage {
        ScreenHeader("Learn checkers", onBack = onBack, backLabel = "Back")
        Text("Short lessons on the rules. You make each move yourself on the board.", style = MaterialTheme.typography.bodyLarge)
        CheckersLessons.all.forEachIndexed { i, lesson ->
            ChoiceCard(
                title = "${i + 1}. ${lesson.title}" + if (lesson.id in done) "  ✓" else "",
                subtitle = lesson.summary,
                onClick = { onOpen(lesson) },
                highlighted = lesson.id in done,
            )
        }
    }
}

@Composable
fun CheckersLessonScreen(vm: CheckersLessonViewModel, onComplete: () -> Unit, onBack: () -> Unit) {
    val state = vm.session.state
    val step = vm.lesson.steps[vm.stepIndex]
    val finished = state.isFinished && vm.isLastStep
    LaunchedEffect(finished) { if (finished) onComplete() }

    BoardScreenLayout(
        header = { ScreenHeader(vm.lesson.title, onBack = onBack, backLabel = "Lessons") },
        info = {
            if (vm.lesson.steps.size > 1) Text("Step ${vm.stepIndex + 1} of ${vm.lesson.steps.size}", style = MaterialTheme.typography.bodyMedium)
            when (state.phase) {
                ExercisePhase.SOLVED, ExercisePhase.SOLUTION_SHOWN -> StatusBanner("Well done!", StatusColors.Good)
                ExercisePhase.WRONG_MOVE -> StatusBanner("Not quite — try again", StatusColors.Alert)
                else -> {}
            }
            Text(
                if (state.isFinished) step.success else step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            )
        },
        board = { modifier -> CheckersExerciseBoard(vm.session, modifier) },
        actions = { stacked ->
            ButtonGroup(stacked) { m ->
                when {
                    finished -> {
                        BigButton("Again", vm::restartStep, m, icon = Icons.Filled.Refresh)
                        BigButton("All lessons", onBack, m)
                    }
                    state.isFinished -> BigButton("Next step", vm::nextStep, m, icon = Icons.AutoMirrored.Filled.ArrowForward)
                    else -> BigButton(
                        "Show me", vm.session::showSolution, m,
                        enabled = state.phase == ExercisePhase.YOUR_TURN || state.phase == ExercisePhase.WRONG_MOVE,
                    )
                }
            }
        },
    )
}
