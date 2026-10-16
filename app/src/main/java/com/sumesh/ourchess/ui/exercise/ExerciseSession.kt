package com.sumesh.ourchess.ui.exercise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumesh.ourchess.game.ChessGame
import com.sumesh.ourchess.game.GameStatus
import com.sumesh.ourchess.game.Move
import com.sumesh.ourchess.game.Piece
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.board.MoveAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ExercisePhase { OPPONENT_SETUP, YOUR_TURN, OPPONENT_REPLY, WRONG_MOVE, SOLVED, SHOWING_SOLUTION, SOLUTION_SHOWN }

data class ExerciseUiState(
    val pieces: Map<Square, Piece>,
    val userSide: Side,
    val phase: ExercisePhase,
    val selected: Square?,
    val targets: Set<Square>,
    val lastMove: Move?,
    val checkSquare: Square?,
    val animation: MoveAnimation?,
    val pendingPromotion: Pair<Square, Square>?,
) {
    val isFinished: Boolean get() = phase == ExercisePhase.SOLVED || phase == ExercisePhase.SOLUTION_SHOWN
}

/**
 * A position where the player must find specific moves (a puzzle or a lesson step).
 *
 * @param setupMove an opponent move played first (puzzles start this way), or null.
 * @param solution the moves to find, in UCI: player, reply, player, ...
 * @param onFinished called once, with true if solved without hints or the solution.
 */
class ExerciseSession(
    private val scope: CoroutineScope,
    fen: String,
    private val setupMove: String?,
    private val solution: List<String>,
    private val onFinished: (solvedUnaided: Boolean) -> Unit = {},
) {
    private val game = ChessGame(fen)
    private val userSide = if (setupMove != null) game.sideToMove.opposite else game.sideToMove
    private var step = 0
    private var phase = if (setupMove != null) ExercisePhase.OPPONENT_SETUP else ExercisePhase.YOUR_TURN
    private var selected: Square? = null
    private var hintTarget: Square? = null
    private var pendingPromotion: Pair<Square, Square>? = null
    private var animation: MoveAnimation? = null
    private var nextAnimationId = 0
    private var helpUsed = false
    private var job: Job? = null

    var state by mutableStateOf(buildState())
        private set

    init {
        if (setupMove != null) {
            job = scope.launch {
                delay(800)
                playAnimated(Move.parseUci(setupMove), OPPONENT_MOVE_MS)
                phase = ExercisePhase.YOUR_TURN
                publish()
            }
        }
    }

    fun onSquareTapped(square: Square) {
        if (phase != ExercisePhase.YOUR_TURN) return
        val current = selected
        when {
            current != null && square in targetsFor(current) -> tryMove(current, square, animate = true)
            game.pieceAt(square)?.side == userSide -> select(if (current == square) null else square)
            else -> select(null)
        }
        publish()
    }

    fun onDragStart(square: Square): Boolean {
        if (phase != ExercisePhase.YOUR_TURN || game.pieceAt(square)?.side != userSide) return false
        select(square)
        publish()
        return true
    }

    fun onDrop(from: Square, to: Square) {
        if (phase == ExercisePhase.YOUR_TURN && to in targetsFor(from)) tryMove(from, to, animate = false)
        publish()
    }

    fun onPromotionChosen(type: PieceType) {
        val (from, to) = pendingPromotion ?: return
        pendingPromotion = null
        playUserMove(Move(from, to, type), animate = true)
    }

    fun onPromotionCancelled() {
        pendingPromotion = null
        publish()
    }

    /** Highlights the piece to move and where it should go. */
    fun hint() {
        if (phase != ExercisePhase.YOUR_TURN) return
        val expected = Move.parseUci(solution[step])
        helpUsed = true
        selected = expected.from
        hintTarget = expected.to
        publish()
    }

    /** Plays out the rest of the solution slowly. */
    fun showSolution() {
        if (state.isFinished || phase == ExercisePhase.SHOWING_SOLUTION) return
        job?.cancel()
        helpUsed = true
        if (phase == ExercisePhase.WRONG_MOVE) game.undo()
        selected = null
        hintTarget = null
        pendingPromotion = null
        phase = ExercisePhase.SHOWING_SOLUTION
        publish()
        job = scope.launch {
            if (setupMove != null && game.moveCount == 0) {
                playAnimated(Move.parseUci(setupMove), OPPONENT_MOVE_MS)
                publish()
            }
            while (step < solution.size) {
                delay(900)
                playAnimated(Move.parseUci(solution[step++]), OPPONENT_MOVE_MS)
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

    private fun select(square: Square?) {
        selected = square
        hintTarget = null
    }

    private fun tryMove(from: Square, to: Square, animate: Boolean) {
        if (game.isPromotion(from, to)) pendingPromotion = from to to else playUserMove(Move(from, to), animate)
    }

    private fun playUserMove(move: Move, animate: Boolean) {
        if (!game.makeMove(move)) return
        select(null)
        animation = if (animate) MoveAnimation(nextAnimationId++, move.from, move.to, USER_MOVE_MS) else null

        val isLastMove = step == solution.size - 1
        // Any checkmate on the final move counts: some puzzles have more than one mating move.
        val correct = move == Move.parseUci(solution[step]) || (isLastMove && game.status() == GameStatus.CHECKMATE)
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
        if (step >= solution.size) {
            finishSolved()
            return
        }
        phase = ExercisePhase.OPPONENT_REPLY
        publish()
        job = scope.launch {
            delay(600)
            playAnimated(Move.parseUci(solution[step++]), OPPONENT_MOVE_MS)
            if (step >= solution.size) finishSolved() else {
                phase = ExercisePhase.YOUR_TURN
                publish()
            }
        }
    }

    private fun finishSolved() {
        phase = ExercisePhase.SOLVED
        publish()
        onFinished(!helpUsed)
    }

    private fun playAnimated(move: Move, durationMs: Int) {
        if (game.makeMove(move)) animation = MoveAnimation(nextAnimationId++, move.from, move.to, durationMs)
    }

    private fun targetsFor(square: Square): Set<Square> = game.legalMovesFrom(square).map { it.to }.toSet()

    private fun publish() {
        state = buildState()
    }

    private fun buildState(): ExerciseUiState {
        val inCheck = game.isInCheck
        return ExerciseUiState(
            pieces = game.pieces(),
            userSide = userSide,
            phase = phase,
            selected = selected,
            targets = hintTarget?.let(::setOf) ?: selected?.let(::targetsFor).orEmpty(),
            lastMove = game.lastMove,
            checkSquare = if (inCheck) game.kingSquare(game.sideToMove) else null,
            animation = animation,
            pendingPromotion = pendingPromotion,
        )
    }

    private companion object {
        const val USER_MOVE_MS = 250
        const val OPPONENT_MOVE_MS = 650
    }
}
