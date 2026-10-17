package com.sumesh.ourchess.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.ludo.LudoGeometry
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.feedback.MoveFeedback
import kotlinx.coroutines.delay

@Composable
fun LudoGameScreen(vm: LudoGameViewModel, onBack: () -> Unit, onNewGame: () -> Unit) {
    val state = vm.state
    val turnColor = state.currentSeat.color.main()

    BoardScreenLayout(
        header = { ScreenHeader("Ludo", onBack = onBack, backLabel = "Back") },
        info = { TurnBanner(state.message, turnColor) },
        board = { modifier ->
            LudoBoard(state.game, state.movable, state.animation, vm::tokenTapped, modifier)
        },
        belowBoard = { Players(state) },
        actions = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LudoDice(
                    face = state.diceFace,
                    color = turnColor,
                    enabled = state.canRoll,
                    onRoll = vm::roll,
                    label = if (state.canRoll) "Tap to roll" else null,
                )
            }
        },
    )

    MoveFeedback(state.lastEvent, opponentName = "Computer")
    if (state.showGameOver) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(800); visible = true }
        if (visible) {
            val winner = state.winnerSeat!!
            AlertDialog(
                onDismissRequest = vm::dismissGameOver,
                title = {
                    Text(
                        if (winner.computer) "The computer (${winner.color.label}) won" else "${winner.name} won!",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                text = { Text("All four ${winner.color.label.lowercase()} tokens are home.", style = MaterialTheme.typography.bodyLarge) },
                confirmButton = {
                    Button(onClick = onNewGame, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("New game", style = MaterialTheme.typography.labelLarge)
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissGameOver, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("Look at the board", style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
        }
    }
}

/** Whose turn it is, in their colour, so it's obvious who should take the tablet. */
@Composable
fun TurnBanner(text: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(28.dp).background(color, CircleShape))
            Text(text, style = MaterialTheme.typography.titleLarge, color = Color(0xFF1F1F1F), textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun Players(state: LudoUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((i, seat) in state.seats.withIndex()) {
            val home = state.game.tokens.getValue(seat.color).count { it == LudoGeometry.HOME }
            val current = i == state.game.current && state.game.winner == null
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (current) seat.color.light() else Color.Transparent, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(22.dp).background(seat.color.main(), CircleShape))
                Text(
                    if (seat.computer) "Computer" else seat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text("$home of 4 home", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
