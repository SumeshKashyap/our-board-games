package com.sumesh.ourchess.ui.checkers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.checkers.CheckersAi
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersGame
import com.sumesh.ourchess.checkers.CheckersLevels
import com.sumesh.ourchess.checkers.CheckersMove
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.checkers.CheckersStatus
import com.sumesh.ourchess.data.Outcome
import com.sumesh.ourchess.data.PlayerSettings
import com.sumesh.ourchess.game.MoveEvent
import com.sumesh.ourchess.game.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CheckersUiState(
    val pieces: Map<Square, CheckersPiece>,
    val humanColor: CheckersColor,
    val toMove: CheckersColor,
    val status: CheckersStatus,
    val computerThinking: Boolean,
    val selected: Square?,
    val partialPath: List<Square>,
    val targets: Set<Square>,
    val lastPath: List<Square>?,
    val animation: CheckersAnimation?,
    val humanLost: List<CheckersPiece>,
    val computerLost: List<CheckersPiece>,
    val canUndo: Boolean,
    val hintLoading: Boolean,
    val hintShown: Boolean,
    val showGameOver: Boolean,
    val pendingConfirm: CheckersMove?,
    val pendingConfirmText: String?,
    val lastMoveEvent: MoveEvent?,
) {
    val isHumanTurn: Boolean get() = !status.isOver && toMove == humanColor && !computerThinking
    val humanWon: Boolean get() = status == if (humanColor == CheckersColor.WHITE) CheckersStatus.WHITE_WINS else CheckersStatus.BLACK_WINS
}

/** A game of checkers against the computer. Saved as text: "W|c3-d4 f6-e5 …" (the player's colour, then the moves). */
class CheckersGameViewModel(
    private val level: Int,
    saved: String? = null,
    private val onSave: (String) -> Unit = {},
    private val onResult: (Outcome) -> Unit = {},
    private val settings: () -> PlayerSettings = { PlayerSettings() },
    private val ai: CheckersAi = CheckersAi(),
) : ViewModel() {

    private var game = CheckersGame()
    private var humanColor = CheckersColor.WHITE
    private val input = CheckersInput()
    private var thinking = false
    private var animation: CheckersAnimation? = null
    private var nextId = 0
    private var engineJob: Job? = null
    private var hintJob: Job? = null
    private var hintLoading = false
    private var hint: CheckersMove? = null
    private var gameOverDismissed = false
    private var resultRecorded = false
    private var pendingConfirm: CheckersMove? = null
    private var pendingConfirmText: String? = null
    private var lastMoveEvent: MoveEvent? = null

    var state by mutableStateOf(buildState())
        private set

    init {
        if (saved != null) {
            humanColor = if (saved.startsWith("B")) CheckersColor.BLACK else CheckersColor.WHITE
            saved.substringAfter('|').split(' ').filter { it.isNotBlank() }.forEach { game.makeMove(it) }
            // An already-finished game isn't announced or counted again.
            gameOverDismissed = game.status().isOver
            resultRecorded = gameOverDismissed
            publish()
            maybePlayComputerMove()
        }
    }

    fun onSquareTapped(square: Square) {
        if (!state.isHumanTurn) return
        if (pendingConfirm != null) clearConfirm()
        hint = null
        val move = input.tap(square, game.position, humanColor)
        if (move != null) requestMove(move)
        publish()
    }

    fun confirmMove() {
        val move = pendingConfirm ?: return
        clearConfirm()
        playHumanMove(move)
    }

    fun cancelMove() {
        clearConfirm()
        publish()
    }

    fun hint() {
        if (!state.isHumanTurn || hintLoading) return
        hintLoading = true
        publish()
        val position = game.position
        hintJob = viewModelScope.launch {
            val move = withContext(Dispatchers.Default) { ai.bestMove(position, CheckersLevels.hint) }
            hintLoading = false
            if (move != null && game.position == position) {
                input.clear()
                hint = move
            }
            publish()
        }
    }

    fun undo() {
        if (!state.canUndo) return
        cancelJobs()
        thinking = false
        animation = null
        game.undo()
        while (game.toMove != humanColor && game.moveCount > 0) game.undo()
        gameOverDismissed = false
        resultRecorded = false
        save()
        publish()
        maybePlayComputerMove()
    }

    fun newGame(playAs: CheckersColor) {
        cancelJobs()
        game = CheckersGame()
        humanColor = playAs
        thinking = false
        animation = null
        gameOverDismissed = false
        resultRecorded = false
        save()
        publish()
        maybePlayComputerMove()
    }

    fun playAgain() = newGame(humanColor)

    fun dismissGameOver() {
        gameOverDismissed = true
        publish()
    }

    private fun requestMove(move: CheckersMove) {
        if (settings().confirmMoves) {
            pendingConfirm = move
            pendingConfirmText = describeCheckersMove(game.position, move)
        } else {
            playHumanMove(move)
        }
    }

    private fun clearConfirm() {
        pendingConfirm = null
        pendingConfirmText = null
        input.clear()
    }

    private fun cancelJobs() {
        engineJob?.cancel()
        hintJob?.cancel()
        hintLoading = false
        hint = null
        clearConfirm()
    }

    private fun play(move: CheckersMove, byComputer: Boolean) {
        val description = describeCheckersMove(game.position, move)
        if (!game.makeMove(move)) return
        animation = CheckersAnimation(nextId++, move.path, if (byComputer) 450 * (move.path.size - 1) else 220 * (move.path.size - 1))
        lastMoveEvent = MoveEvent(nextId++, description, byOpponent = byComputer, capture = move.isCapture)
        save()
        checkResult()
    }

    private fun playHumanMove(move: CheckersMove) {
        hintJob?.cancel()
        hintLoading = false
        hint = null
        input.clear()
        play(move, byComputer = false)
        publish()
        maybePlayComputerMove()
    }

    private fun maybePlayComputerMove() {
        if (game.status().isOver || game.toMove == humanColor) return
        thinking = true
        publish()
        engineJob = viewModelScope.launch {
            val started = System.currentTimeMillis()
            val position = game.position
            val move = withContext(Dispatchers.Default) { ai.bestMove(position, CheckersLevels.strength(level)) }
            delay((MIN_THINK_MS - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            thinking = false
            if (move != null) play(move, byComputer = true)
            publish()
        }
    }

    private fun checkResult() {
        val status = game.status()
        if (!status.isOver || resultRecorded) return
        resultRecorded = true
        val humanWins = if (humanColor == CheckersColor.WHITE) CheckersStatus.WHITE_WINS else CheckersStatus.BLACK_WINS
        onResult(
            when (status) {
                CheckersStatus.DRAW -> Outcome.DRAWN
                humanWins -> Outcome.WON
                else -> Outcome.LOST
            },
        )
    }

    private fun save() {
        val color = if (humanColor == CheckersColor.WHITE) "W" else "B"
        onSave("$color|" + game.moves.joinToString(" ") { it.notation })
    }

    private fun publish() {
        state = buildState()
    }

    private fun buildState(): CheckersUiState {
        val status = game.status()
        val legal = if (status.isOver) emptyList() else game.position.legalMoves()
        val confirm = pendingConfirm
        return CheckersUiState(
            pieces = game.position.pieces,
            humanColor = humanColor,
            toMove = game.toMove,
            status = status,
            computerThinking = thinking,
            selected = confirm?.from ?: hint?.from ?: input.selected,
            partialPath = confirm?.path?.drop(1)?.dropLast(1) ?: input.path.drop(1),
            targets = (confirm ?: hint)?.let { setOf(it.to) } ?: input.targets(legal),
            lastPath = game.lastMove?.path,
            animation = animation,
            humanLost = game.lost(humanColor),
            computerLost = game.lost(humanColor.opposite),
            canUndo = game.moveCount >= (if (humanColor == CheckersColor.WHITE) 1 else 2),
            hintLoading = hintLoading,
            hintShown = hint != null,
            showGameOver = status.isOver && !gameOverDismissed,
            pendingConfirm = confirm,
            pendingConfirmText = pendingConfirmText,
            lastMoveEvent = lastMoveEvent,
        )
    }

    private companion object {
        const val MIN_THINK_MS = 700L
    }
}
