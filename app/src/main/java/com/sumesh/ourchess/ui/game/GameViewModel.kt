package com.sumesh.ourchess.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.data.PlayerSettings
import com.sumesh.ourchess.data.SavedGame
import com.sumesh.ourchess.game.MoveEvent
import com.sumesh.ourchess.game.describeMove
import com.sumesh.ourchess.engine.ChessEngine
import com.sumesh.ourchess.engine.RandomMoveEngine
import com.sumesh.ourchess.game.ChessGame
import com.sumesh.ourchess.game.GameStatus
import com.sumesh.ourchess.game.Move
import com.sumesh.ourchess.game.Piece
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.board.MoveAnimation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GameUiState(
    val pieces: Map<Square, Piece>,
    val humanSide: Side,
    val sideToMove: Side,
    val status: GameStatus,
    val inCheck: Boolean,
    val computerThinking: Boolean,
    val selected: Square?,
    val targets: Set<Square>,
    val lastMove: Move?,
    val checkSquare: Square?,
    val animation: MoveAnimation?,
    val pendingPromotion: Pair<Square, Square>?,
    /** Pieces the human has lost / captured. */
    val humanLost: List<PieceType>,
    val computerLost: List<PieceType>,
    val canUndo: Boolean,
    val hintLoading: Boolean,
    /** True while a suggested move is highlighted on the board. */
    val hintShown: Boolean,
    /** True once the game has ended and the player hasn't closed the result dialog yet. */
    val showGameOver: Boolean,
    /** With "Ask before each move" on: the move waiting for Yes/No, and how it reads. */
    val pendingConfirm: Move?,
    val pendingConfirmText: String?,
    /** The latest move, for sound, vibration and speech. */
    val lastMoveEvent: MoveEvent?,
) {
    val isHumanTurn: Boolean get() = !status.isOver && sideToMove == humanSide && !computerThinking

    /** Material balance from the player's point of view (pawn = 1, knight/bishop = 3, rook = 5, queen = 9). */
    val materialBalance: Int get() = computerLost.sumOf { it.value } - humanLost.sumOf { it.value }
}

private val PieceType.value: Int
    get() = when (this) {
        PieceType.QUEEN -> 9
        PieceType.ROOK -> 5
        PieceType.BISHOP, PieceType.KNIGHT -> 3
        PieceType.PAWN -> 1
        PieceType.KING -> 0
    }

class GameViewModel(
    private val engine: ChessEngine = RandomMoveEngine(),
    private val hintEngine: ChessEngine = RandomMoveEngine(thinkTimeMs = 0),
    saved: SavedGame? = null,
    /** Called whenever the moves change, so the game can be saved. */
    private val onSave: (SavedGame) -> Unit = {},
    /** The current player's settings (read each time, so changes apply straight away). */
    private val settings: () -> PlayerSettings = { PlayerSettings() },
) : ViewModel() {

    private var game = ChessGame()
    private var humanSide = Side.WHITE
    private var selected: Square? = null
    private var pendingPromotion: Pair<Square, Square>? = null
    private var thinking = false
    private var animation: MoveAnimation? = null
    private var nextAnimationId = 0
    private var engineJob: Job? = null
    private var hintJob: Job? = null
    private var hintLoading = false
    private var hintTarget: Square? = null
    private var gameOverDismissed = false
    private var pendingConfirm: Move? = null
    private var pendingConfirmText: String? = null
    private var lastMoveEvent: MoveEvent? = null
    private var nextEventId = 0

    var state by mutableStateOf(buildState())
        private set

    init {
        if (saved != null) {
            humanSide = saved.humanSide
            saved.moves.forEach { if (!game.makeUciMove(it)) return@forEach }
            gameOverDismissed = game.status().isOver // don't re-announce an old result
            publish()
            maybePlayComputerMove()
        }
    }

    fun onSquareTapped(square: Square) {
        if (!state.isHumanTurn) return
        // Tapping the board while a move awaits confirmation means "no, not that one".
        if (pendingConfirm != null) clearConfirm()
        val current = selected
        when {
            current != null && square in targetsFor(current) -> tryMove(current, square, animate = true)
            game.pieceAt(square)?.side == humanSide -> select(if (current == square) null else square)
            else -> select(null)
        }
        publish()
    }

    /** Returns true if the piece on [square] may be dragged. */
    fun onDragStart(square: Square): Boolean {
        if (!state.isHumanTurn || game.pieceAt(square)?.side != humanSide) return false
        select(square)
        publish()
        return true
    }

    fun onDrop(from: Square, to: Square) {
        if (state.isHumanTurn && to in targetsFor(from)) tryMove(from, to, animate = false)
        publish()
    }

    fun onPromotionChosen(type: PieceType) {
        val (from, to) = pendingPromotion ?: return
        pendingPromotion = null
        requestMove(Move(from, to, type), animate = true)
    }

    /** "Yes, play it" for a move waiting for confirmation. */
    fun confirmMove() {
        val move = pendingConfirm ?: return
        clearConfirm()
        playHumanMove(move, animate = true)
    }

    fun cancelMove() {
        clearConfirm()
        publish()
    }

    fun onPromotionCancelled() {
        pendingPromotion = null
        publish()
    }

    /** Asks the engine for a good move and highlights it. */
    fun hint() {
        if (!state.isHumanTurn || hintLoading) return
        hintLoading = true
        publish()
        val fen = game.fen
        hintJob = viewModelScope.launch {
            val move = hintEngine.bestMove(fen)?.let(Move::parseUci)
            hintLoading = false
            if (move != null && game.fen == fen) {
                selected = move.from
                hintTarget = move.to
            }
            publish()
        }
    }

    /** Takes back your last move (and the computer's reply to it). */
    fun undo() {
        if (!state.canUndo) return
        cancelJobs()
        thinking = false
        select(null)
        pendingPromotion = null
        animation = null
        game.undo()
        while (game.sideToMove != humanSide && game.moveCount > 0) game.undo()
        gameOverDismissed = false
        save()
        publish()
        maybePlayComputerMove()
    }

    fun newGame(playAs: Side) {
        cancelJobs()
        game = ChessGame()
        humanSide = playAs
        thinking = false
        select(null)
        pendingPromotion = null
        animation = null
        gameOverDismissed = false
        save()
        publish()
        maybePlayComputerMove()
    }

    fun dismissGameOver() {
        gameOverDismissed = true
        publish()
    }

    /** Starts a new game with the same colours. */
    fun playAgain() = newGame(humanSide)

    private fun save() = onSave(SavedGame(humanSide, game.moves.map { it.uci }))

    private fun select(square: Square?) {
        selected = square
        hintTarget = null
    }

    private fun cancelJobs() {
        engineJob?.cancel()
        hintJob?.cancel()
        hintLoading = false
        clearConfirm()
    }

    private fun clearConfirm() {
        pendingConfirm = null
        pendingConfirmText = null
        select(null)
    }

    private fun tryMove(from: Square, to: Square, animate: Boolean) {
        when {
            !game.isPromotion(from, to) -> requestMove(Move(from, to), animate)
            settings().autoQueen -> requestMove(Move(from, to, PieceType.QUEEN), animate)
            else -> pendingPromotion = from to to
        }
    }

    /** Plays the move, or first asks "Play this move?" if the player wants to confirm moves. */
    private fun requestMove(move: Move, animate: Boolean) {
        if (settings().confirmMoves) {
            pendingConfirm = move
            pendingConfirmText = describeMove(game, move)
            selected = move.from
            hintTarget = move.to
            publish()
        } else {
            playHumanMove(move, animate)
        }
    }

    private fun playHumanMove(move: Move, animate: Boolean) {
        val description = describeMove(game, move)
        val capture = game.pieceAt(move.to) != null || description.contains(" takes ")
        if (!game.makeMove(move)) return
        lastMoveEvent = MoveEvent(nextEventId++, description, byOpponent = false, capture = capture)
        hintJob?.cancel()
        hintLoading = false
        select(null)
        animation = if (animate) MoveAnimation(nextAnimationId++, move.from, move.to, HUMAN_MOVE_MS) else null
        save()
        publish()
        maybePlayComputerMove()
    }

    private fun maybePlayComputerMove() {
        if (game.status().isOver || game.sideToMove == humanSide) return
        thinking = true
        publish()
        engineJob = viewModelScope.launch {
            val started = System.currentTimeMillis()
            val uci = engine.bestMove(game.fen)
            // Even when the engine answers instantly, pause briefly so the reply doesn't feel abrupt.
            delay((MIN_THINK_MS - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            thinking = false
            val move = uci?.let(Move::parseUci)
            val description = move?.let { describeMove(game, it) }
            if (move != null && game.makeMove(move)) {
                lastMoveEvent = MoveEvent(nextEventId++, description!!, byOpponent = true, capture = description.contains(" takes "))
                animation = MoveAnimation(nextAnimationId++, move.from, move.to, COMPUTER_MOVE_MS)
                save()
            }
            publish()
        }
    }

    private fun targetsFor(square: Square): Set<Square> = game.legalMovesFrom(square).map { it.to }.toSet()

    private fun publish() {
        state = buildState()
    }

    private fun buildState(): GameUiState {
        val status = game.status()
        val inCheck = game.isInCheck
        val humanMovesNeeded = if (humanSide == Side.WHITE) 1 else 2
        return GameUiState(
            pieces = game.pieces(),
            humanSide = humanSide,
            sideToMove = game.sideToMove,
            status = status,
            inCheck = inCheck,
            computerThinking = thinking,
            selected = selected,
            targets = hintTarget?.let(::setOf) ?: selected?.let(::targetsFor).orEmpty(),
            lastMove = game.lastMove,
            checkSquare = if (inCheck) game.kingSquare(game.sideToMove) else null,
            animation = animation,
            pendingPromotion = pendingPromotion,
            humanLost = game.lostPieces(humanSide),
            computerLost = game.lostPieces(humanSide.opposite),
            canUndo = game.moveCount >= humanMovesNeeded,
            hintLoading = hintLoading,
            hintShown = hintTarget != null && pendingConfirm == null,
            showGameOver = status.isOver && !gameOverDismissed,
            pendingConfirm = pendingConfirm,
            pendingConfirmText = pendingConfirmText,
            lastMoveEvent = lastMoveEvent,
        )
    }

    private companion object {
        const val HUMAN_MOVE_MS = 250
        // Slow enough for the player to see clearly what the computer did.
        const val COMPUTER_MOVE_MS = 650
        const val MIN_THINK_MS = 700L
    }
}
