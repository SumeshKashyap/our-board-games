package com.sumesh.ourchess.ui.ludo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.game.MoveEvent
import com.sumesh.ourchess.ludo.LudoAi
import com.sumesh.ourchess.ludo.LudoEvent
import com.sumesh.ourchess.ludo.LudoGeometry
import com.sumesh.ourchess.ludo.LudoRules
import com.sumesh.ourchess.ludo.LudoSaved
import com.sumesh.ourchess.ludo.LudoSeat
import com.sumesh.ourchess.ludo.LudoState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LudoUiState(
    val game: LudoState,
    val seats: List<LudoSeat>,
    val rolling: Boolean,
    /** The face shown on the dice (the last roll, or a changing face while rolling). */
    val diceFace: Int,
    val message: String,
    val movable: Set<Int>,
    val animation: LudoAnimation?,
    val lastEvent: MoveEvent?,
    val showGameOver: Boolean,
) {
    val currentSeat: LudoSeat get() = seats[game.current]
    val winnerSeat: LudoSeat? get() = game.winner?.let { w -> seats.first { it.color == w } }
    /** A family member may roll now. */
    val canRoll: Boolean get() = game.winner == null && !currentSeat.computer && game.dice == null && !rolling
    val busy: Boolean get() = rolling || animation != null
}

/**
 * A Ludo game for 2–4 seats, each a family member (passing the device) or the computer.
 * [level] sets how well the computer plays. [onFinished] reports the winner once.
 */
class LudoGameViewModel(
    seats: List<LudoSeat>,
    private val level: Int,
    saved: LudoSaved? = null,
    private val onSave: (LudoSaved) -> Unit = {},
    private val onFinished: (winner: LudoSeat, seats: List<LudoSeat>) -> Unit = { _, _ -> },
    private val random: Random = Random.Default,
    private val stepDelayMs: Long = 900,
) : ViewModel() {

    private val seats = saved?.seats ?: seats
    private var game = saved?.state ?: LudoState.newGame(this.seats.map { it.color })
    private val ai = LudoAi(random)
    private var rolling = false
    private var diceFace = game.dice ?: 6
    private var message = ""
    private var animation: LudoAnimation? = null
    private var lastEvent: MoveEvent? = null
    private var nextId = 0
    private var gameOverDismissed = game.winner != null
    private var job: Job? = null

    var state by mutableStateOf(buildState())
        private set

    init {
        message = promptFor()
        publish()
        continueTurn()
    }

    fun roll() {
        if (!state.canRoll) return
        doRoll()
    }

    fun tokenTapped(token: Int) {
        if (state.currentSeat.computer || game.dice == null || state.busy) return
        if (token in game.movableTokens()) doMove(token)
    }

    fun dismissGameOver() {
        gameOverDismissed = true
        publish()
    }

    private fun name(seatIndex: Int = game.current): String = seats[seatIndex].let { s ->
        if (s.computer) "Computer (${s.color.label})" else "${s.name} (${s.color.label})"
    }

    private fun promptFor(): String = when {
        game.winner != null -> "${state.winnerSeat?.name ?: ""} won!"
        seats[game.current].computer -> "${name()} is playing…"
        game.dice != null -> "${name()}: tap a token to move ${game.dice}"
        else -> "${name()}: tap the dice to roll"
    }

    private fun doRoll() {
        rolling = true
        publish()
        job = viewModelScope.launch {
            // A short tumble of random faces, then the real roll.
            repeat(6) {
                diceFace = random.nextInt(1, 7)
                publish()
                delay(80)
            }
            val value = random.nextInt(1, 7)
            diceFace = value
            rolling = false
            val roller = game.current
            val (next, event) = LudoRules.roll(game, value)
            game = next
            lastEvent = MoveEvent(nextId++, "${seats[roller].color.label} rolled $value", seats[roller].computer, capture = false)
            message = when {
                event.lostTurnOnThreeSixes -> "Three 6s in a row — ${name(roller)} loses the turn"
                !event.canMove -> "${name(roller)} rolled $value — no token can move"
                else -> promptFor()
            }
            save()
            publish()
            if (!event.canMove) {
                delay(stepDelayMs * 3 / 2)
                message = promptFor()
                publish()
                continueTurn()
            } else {
                val movable = game.movableTokens()
                // With only one choice, move it for the player (after a moment to see the roll).
                if (seats[game.current].computer || movable.size == 1) {
                    delay(stepDelayMs)
                    doMove(if (seats[game.current].computer) ai.choose(game, level)!! else movable.single())
                }
            }
        }
    }

    private fun doMove(token: Int) {
        val mover = game.current
        val (next, event) = LudoRules.move(game, token)
        game = next
        animation = LudoAnimation(nextId++, event.color, token, event.from, event.to)
        val captureText = event.captured.joinToString { (c, _) -> c.label }
        lastEvent = MoveEvent(
            nextId++,
            when {
                event.captured.isNotEmpty() -> "${event.color.label} captured $captureText"
                event.to == LudoGeometry.HOME -> "${event.color.label} token reached home"
                else -> "${event.color.label} moved"
            },
            byOpponent = seats[mover].computer,
            capture = event.captured.isNotEmpty(),
        )
        message = when {
            game.winner != null -> "${seats[mover].name} won!"
            event.captured.isNotEmpty() -> "${name(mover)} captured $captureText! Roll again"
            event.extraTurn -> "${name(mover)}: roll again"
            else -> promptFor()
        }
        save()
        publish()
        job = viewModelScope.launch {
            val hops = if (event.from == LudoGeometry.YARD) 1 else event.to - event.from
            delay(140L * hops + 200)
            animation = null
            if (game.winner != null) {
                onFinished(seats[mover], seats)
            } else if (!event.extraTurn) {
                message = promptFor()
            }
            publish()
            continueTurn()
        }
    }

    /** Lets the computer play its turn(s); waits for family members to roll. */
    private fun continueTurn() {
        if (game.winner != null || !seats[game.current].computer || game.dice != null) return
        job = viewModelScope.launch {
            message = promptFor()
            publish()
            delay(stepDelayMs)
            doRoll()
        }
    }

    private fun save() = onSave(LudoSaved(seats, game))

    private fun publish() {
        state = buildState()
    }

    private fun buildState() = LudoUiState(
        game = game,
        seats = seats,
        rolling = rolling,
        diceFace = diceFace,
        message = message,
        movable = if (game.dice != null && !seats[game.current].computer && animation == null) game.movableTokens().toSet() else emptySet(),
        animation = animation,
        lastEvent = lastEvent,
        showGameOver = game.winner != null && !gameOverDismissed && animation == null,
    )
}
