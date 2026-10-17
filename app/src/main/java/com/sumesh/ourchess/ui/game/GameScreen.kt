@file:OptIn(ExperimentalLayoutApi::class)

package com.sumesh.ourchess.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.game.GameStatus
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.board.ChessBoard
import com.sumesh.ourchess.ui.board.displayName
import com.sumesh.ourchess.ui.board.pieceDrawable
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ButtonGroup
import com.sumesh.ourchess.ui.common.PromotionDialog
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.common.StatusBanner
import com.sumesh.ourchess.ui.common.StatusColors
import com.sumesh.ourchess.ui.feedback.MoveFeedback
import kotlinx.coroutines.delay

@Composable
fun GameScreen(vm: GameViewModel, level: Int, onBack: () -> Unit) {
    val state = vm.state
    var askNewGame by remember { mutableStateOf(false) }

    BoardScreenLayout(
        header = { ScreenHeader("Level $level · ${Levels.name(level)}", onBack = onBack, backLabel = "Menu") },
        info = {
            if (state.pendingConfirm != null) {
                StatusBanner("Play: ${state.pendingConfirmText}?", StatusColors.Neutral)
            } else {
                val (text, color) = statusText(state)
                StatusBanner(text, color)
            }
        },
        board = { modifier ->
            ChessBoard(
                pieces = state.pieces,
                flipped = state.humanSide == Side.BLACK,
                selected = state.selected,
                targets = state.targets,
                lastMove = state.lastMove,
                checkSquare = state.checkSquare,
                animation = state.animation,
                targetsAreHint = state.hintShown || state.pendingConfirm != null,
                onSquareTap = vm::onSquareTapped,
                onDragStart = vm::onDragStart,
                onDrop = vm::onDrop,
                modifier = modifier,
            )
        },
        belowBoard = { CapturedPieces(state) },
        actions = { stacked ->
            if (state.pendingConfirm != null) {
                ButtonGroup(stacked) { m ->
                    Button(onClick = vm::confirmMove, modifier = m.heightIn(min = 64.dp)) {
                        Text("Yes, play it", style = MaterialTheme.typography.labelLarge)
                    }
                    BigButton("No", vm::cancelMove, m)
                }
                return@BoardScreenLayout
            }
            ButtonGroup(stacked) { m ->
                BigButton("Undo", vm::undo, m, enabled = state.canUndo)
                BigButton(if (state.hintLoading) "Thinking…" else "Hint", vm::hint, m, enabled = state.isHumanTurn && !state.hintLoading)
                BigButton("New Game", { askNewGame = true }, m)
            }
        },
    )

    MoveFeedback(state.lastMoveEvent, opponentName = "Computer")
    state.pendingPromotion?.let {
        PromotionDialog(side = state.humanSide, onChoose = vm::onPromotionChosen, onDismiss = vm::onPromotionCancelled)
    }
    if (state.showGameOver) {
        GameOverDialog(state, onPlayAgain = vm::playAgain, onClose = vm::dismissGameOver)
    }
    if (askNewGame) {
        NewGameDialog(
            onChoose = { side -> askNewGame = false; vm.newGame(side) },
            onDismiss = { askNewGame = false },
        )
    }
}

private fun statusText(state: GameUiState): Pair<String, Color> = when (state.status) {
    GameStatus.CHECKMATE ->
        if (state.sideToMove != state.humanSide) "Checkmate — you won!" to StatusColors.Good
        else "Checkmate — the computer won" to StatusColors.Alert
    GameStatus.STALEMATE -> "Draw — stalemate" to StatusColors.Neutral
    GameStatus.DRAW_REPETITION -> "Draw — same position 3 times" to StatusColors.Neutral
    GameStatus.DRAW_FIFTY_MOVES -> "Draw — 50 moves without a capture" to StatusColors.Neutral
    GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw — not enough pieces to win" to StatusColors.Neutral
    GameStatus.ONGOING -> when {
        state.computerThinking -> "Computer is thinking…" to StatusColors.Neutral
        state.inCheck -> "Check! Your move" to StatusColors.Alert
        state.hintShown -> "Hint: try the highlighted move" to StatusColors.Good
        else -> "Your move" to StatusColors.Good
    }
}

@Composable
private fun CapturedPieces(state: GameUiState) {
    val balance = state.materialBalance
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CapturedRow("You captured:", state.computerLost, state.humanSide.opposite, lead = balance.takeIf { it > 0 })
        CapturedRow("Computer captured:", state.humanLost, state.humanSide, lead = (-balance).takeIf { it > 0 })
    }
}

/** [lead]: how many points ahead this side is, shown as "+3" so it's easy to see who is winning. */
@Composable
private fun CapturedRow(label: String, pieces: List<PieceType>, side: Side, lead: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
        FlowRow(verticalArrangement = Arrangement.Center) {
            // Most valuable first, so the important losses stand out.
            for (type in pieces.sortedBy { it.ordinal }) {
                Image(painterResource(pieceDrawable(side, type)), contentDescription = type.displayName(), Modifier.size(30.dp))
            }
            if (lead != null) {
                Text(
                    "+$lead",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun GameOverDialog(state: GameUiState, onPlayAgain: () -> Unit, onClose: () -> Unit) {
    // Let the final move finish sliding before announcing the result.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        visible = true
    }
    if (!visible) return

    val humanWon = state.status == GameStatus.CHECKMATE && state.sideToMove != state.humanSide
    val (title, message) = when {
        humanWon -> "You won!" to "Checkmate. Very well played!"
        state.status == GameStatus.CHECKMATE -> "The computer won" to "Checkmate. Don't worry — try again, or use Undo to see what went wrong."
        else -> "It's a draw" to statusText(state).first.removePrefix("Draw — ").replaceFirstChar { it.uppercase() } + "."
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(onClick = onPlayAgain, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Play again", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Look at the board", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun NewGameDialog(onChoose: (Side) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a new game?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The current game will end.", style = MaterialTheme.typography.bodyLarge)
                SideButton("Play as White", Side.WHITE, onChoose)
                SideButton("Play as Black", Side.BLACK, onChoose)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SideButton(label: String, side: Side, onChoose: (Side) -> Unit) {
    Button(onClick = { onChoose(side) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Image(painterResource(pieceDrawable(side, PieceType.KING)), contentDescription = null, Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
