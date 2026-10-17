package com.sumesh.ourchess.ui.checkers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersGame
import com.sumesh.ourchess.checkers.CheckersMove
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.checkers.CheckersPosition
import com.sumesh.ourchess.game.MoveEvent
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.exercise.ExercisePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CheckersExerciseState(
    val pieces: Map<Square, CheckersPiece>,
    val userColor: CheckersColor,
    val phase: ExercisePhase,
    val selected: Square?,
    val partialPath: List<Square>,
    val targets: Set<Square>,
    val lastPath: List<Square>?,
    val animation: CheckersAnimation?,
    val hintShown: Boolean,
    val lastMoveEvent: MoveEvent?,
) {
    val isFinished: Boolean get() = phase == ExercisePhase.SOLVED || phase == ExercisePhase.SOLUTION_SHOWN
}

/**
 * A checkers position where the player must find given moves (a puzzle or a lesson step).
 * [solution] alternates player, reply, player, … in move notation ("c3-d4", "c3:e5:g7").
 */
class CheckersExerciseSession(
    private val scope: CoroutineScope,
    start: CheckersPosition,
    private val solution: List<String>,
    private val onFinished: (solvedUnaided: Boolean) -> Unit = {},
) {
    private val game = CheckersGame(start)
    private val userColor = start.toMove
    private val input = CheckersInput()
    private var step = 0
    private var phase = ExercisePhase.YOUR_TURN
    private var hint: CheckersMove? = null
    private var animation: CheckersAnimation? = null
    private var lastMoveEvent: MoveEvent? = null
    private var nextId = 0
    private var helpUsed = false
    private var job: Job? = null

    var state by mutableStateOf(buildState())
        private set

    fun onSquareTapped(square: Square) {
        if (phase != ExercisePhase.YOUR_TURN) return
        hint = null
        val move = input.tap(square, game.position, userColor)
        if (move != null) playUserMove(move)
        publish()
    }

    fun hint() {
        if (phase != ExercisePhase.YOUR_TURN) return
        helpUsed = true
        input.clear()
        hint = expected()
        publish()
    }

    fun showSolution() {
        if (state.isFinished || phase == ExercisePhase.SHOWING_SOLUTION) return
        job?.cancel()
        helpUsed = true
        if (phase == ExercisePhase.WRONG_MOVE) game.undo()
        input.clear()
        hint = null
        phase = ExercisePhase.SHOWING_SOLUTION
        publish()
        job = scope.launch {
            while (step < solution.size) {
                delay(900)
                expected()?.let { play(it, byOpponent = true) }
                step++
                publish()
            }
            phase = ExercisePhase.SOLUTION_SHOWN
            publish()
            onFinished(false)
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun expected(): CheckersMove? = CheckersMove.parse(solution[step], game.position.legalMoves())

    private fun play(move: CheckersMove, byOpponent: Boolean) {
        val description = describeCheckersMove(game.position, move)
        if (!game.makeMove(move)) return
        animation = CheckersAnimation(nextId++, move.path, (if (byOpponent) 450 else 220) * (move.path.size - 1))
        lastMoveEvent = MoveEvent(nextId++, description, byOpponent, move.isCapture)
    }

    private fun playUserMove(move: CheckersMove) {
        val correct = move.notation == solution[step]
        play(move, byOpponent = false)
        if (!correct) {
            phase = ExercisePhase.WRONG_MOVE
            publish()
            job = scope.launch {
                delay(1500)
                game.undo()
                animation = null
                phase = ExercisePhase.YOUR_TURN
                publish()
            }
            return
        }
        step++
        if (step >= solution.size) return finish()
        phase = ExercisePhase.OPPONENT_REPLY
        publish()
        job = scope.launch {
            delay(600)
            expected()?.let { play(it, byOpponent = true) }
            step++
            if (step >= solution.size) finish() else {
                phase = ExercisePhase.YOUR_TURN
                publish()
            }
        }
    }

    private fun finish() {
        phase = ExercisePhase.SOLVED
        publish()
        onFinished(!helpUsed)
    }

    private fun publish() {
        state = buildState()
    }

    private fun buildState(): CheckersExerciseState {
        val legal = game.position.legalMoves()
        return CheckersExerciseState(
            pieces = game.position.pieces,
            userColor = userColor,
            phase = phase,
            selected = hint?.from ?: input.selected,
            partialPath = input.path.drop(1),
            targets = hint?.let { setOf(it.to) } ?: input.targets(legal),
            lastPath = game.lastMove?.path,
            animation = animation,
            hintShown = hint != null || input.path.size > 1,
            lastMoveEvent = lastMoveEvent,
        )
    }
}
