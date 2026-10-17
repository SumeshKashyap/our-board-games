package com.sumesh.ourchess.ui.checkers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.checkers.CheckersStatus
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ButtonGroup
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.common.StatusBanner
import com.sumesh.ourchess.ui.common.StatusColors
import com.sumesh.ourchess.ui.feedback.MoveFeedback
import kotlinx.coroutines.delay

@Composable
fun CheckersGameScreen(vm: CheckersGameViewModel, level: Int, onBack: () -> Unit) {
    val state = vm.state
    var askNewGame by remember { mutableStateOf(false) }

    BoardScreenLayout(
        header = { ScreenHeader("Checkers · ${Levels.name(level)}", onBack = onBack, backLabel = "Back") },
        info = {
            if (state.pendingConfirm != null) {
                StatusBanner("Play: ${state.pendingConfirmText}?", StatusColors.Neutral)
            } else {
                val (text, color) = statusText(state)
                StatusBanner(text, color)
            }
        },
        board = { modifier ->
            CheckersBoard(
                pieces = state.pieces,
                flipped = state.humanColor == CheckersColor.BLACK,
                selected = state.selected,
                partialPath = state.partialPath,
                targets = state.targets,
                lastPath = state.lastPath,
                animation = state.animation,
                // Hints, confirmations and the next hop of a multi-capture always show.
                targetsAreHint = state.hintShown || state.pendingConfirm != null || state.partialPath.isNotEmpty(),
                onSquareTap = vm::onSquareTapped,
                modifier = modifier,
            )
        },
        belowBoard = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CapturedRow("You captured:", state.computerLost)
                CapturedRow("Computer captured:", state.humanLost)
            }
        },
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
    if (state.showGameOver) GameOverDialog(state, onPlayAgain = vm::playAgain, onClose = vm::dismissGameOver)
    if (askNewGame) {
        AlertDialog(
            onDismissRequest = { askNewGame = false },
            title = { Text("Start a new game?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("The current game will end.", style = MaterialTheme.typography.bodyLarge)
                    for (color in CheckersColor.entries) {
                        Button(
                            onClick = { askNewGame = false; vm.newGame(color) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        ) {
                            PieceDisc(CheckersPiece(color), 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (color == CheckersColor.WHITE) "Play as White (moves first)" else "Play as Black",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { askNewGame = false }) { Text("Cancel") } },
        )
    }
}

private fun statusText(state: CheckersUiState): Pair<String, Color> = when {
    state.status == CheckersStatus.DRAW -> "It's a draw" to StatusColors.Neutral
    state.status.isOver -> if (state.humanWon) "You won!" to StatusColors.Good else "The computer won" to StatusColors.Alert
    state.computerThinking -> "Computer is thinking…" to StatusColors.Neutral
    state.partialPath.isNotEmpty() -> "Keep capturing: tap the next square" to StatusColors.Good
    state.hintShown -> "Hint: try the highlighted move" to StatusColors.Good
    else -> "Your move" to StatusColors.Good
}

@Composable
private fun CapturedRow(label: String, pieces: List<CheckersPiece>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
        val kings = pieces.count { it.king }
        val men = pieces.size - kings
        val color = pieces.firstOrNull()?.color
        if (color != null) {
            PieceDisc(CheckersPiece(color), 26.dp)
            Text(" × $men", style = MaterialTheme.typography.titleMedium)
            if (kings > 0) {
                Spacer(Modifier.width(12.dp))
                PieceDisc(CheckersPiece(color, king = true), 26.dp)
                Text(" × $kings", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun PieceDisc(piece: CheckersPiece, size: Dp) {
    Canvas(Modifier.size(size)) {
        drawCheckersPiece(Offset(this.size.width / 2, this.size.height / 2), this.size.minDimension * 0.42f, piece)
    }
}

@Composable
private fun GameOverDialog(state: CheckersUiState, onPlayAgain: () -> Unit, onClose: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        visible = true
    }
    if (!visible) return
    val (title, message) = when {
        state.status == CheckersStatus.DRAW -> "It's a draw" to "Neither side could make progress."
        state.humanWon -> "You won!" to "The computer has no moves left. Very well played!"
        else -> "The computer won" to "You have no moves left. Try again, or use Undo to see what happened."
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
