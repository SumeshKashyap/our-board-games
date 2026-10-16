package com.sumesh.ourchess.ui.puzzles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ButtonGroup
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.common.StatusBanner
import com.sumesh.ourchess.ui.common.StatusColors
import com.sumesh.ourchess.ui.exercise.ExerciseBoard
import com.sumesh.ourchess.ui.exercise.ExercisePhase

@Composable
fun PuzzleScreen(vm: PuzzleViewModel, level: Int, solvedCount: Int, onBack: () -> Unit) {
    val session = vm.session
    val state = session.state
    val color = if (state.userSide == Side.WHITE) "White" else "Black"

    BoardScreenLayout(
        header = { ScreenHeader("Puzzles · ${Levels.name(level)}", onBack = onBack, backLabel = "Menu") },
        info = {
            val (text, bg) = when (state.phase) {
                ExercisePhase.OPPONENT_SETUP -> "Watch your opponent's move…" to StatusColors.Neutral
                ExercisePhase.YOUR_TURN -> "You play $color:\n${vm.puzzle.goal}" to StatusColors.Good
                ExercisePhase.OPPONENT_REPLY -> "Good move! Opponent replies…" to StatusColors.Good
                ExercisePhase.WRONG_MOVE -> "Not quite — try again" to StatusColors.Alert
                ExercisePhase.SOLVED -> "Well done — solved!" to StatusColors.Good
                ExercisePhase.SHOWING_SOLUTION, ExercisePhase.SOLUTION_SHOWN -> "Here is the solution" to StatusColors.Neutral
            }
            StatusBanner(text, bg)
            Text(
                "Puzzle ${vm.index + 1} of ${vm.count}  ·  Solved: $solvedCount",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        board = { modifier -> ExerciseBoard(session, modifier) },
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
