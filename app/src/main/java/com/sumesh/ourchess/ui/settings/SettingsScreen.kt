package com.sumesh.ourchess.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.BoardTheme
import com.sumesh.ourchess.data.PlayerSettings
import com.sumesh.ourchess.game.ChessGame
import com.sumesh.ourchess.game.MoveEvent
import com.sumesh.ourchess.game.Square
import com.sumesh.ourchess.ui.board.ChessBoard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.feedback.MoveFeedback
import com.sumesh.ourchess.ui.theme.palette

@Composable
fun SettingsScreen(name: String, settings: PlayerSettings, onChange: (PlayerSettings) -> Unit, onBack: () -> Unit) {
    // A sample move is played through the feedback when sound, vibration or speech is switched on,
    // so the player immediately hears/feels what it does.
    var sampleId by remember { mutableIntStateOf(0) }
    var sample by remember { mutableStateOf<MoveEvent?>(null) }
    fun change(new: PlayerSettings, tryIt: Boolean = false) {
        onChange(new)
        if (tryIt) sample = MoveEvent(++sampleId, "Knight to f3", byOpponent = true, capture = false)
    }
    MoveFeedback(sample, opponentName = "Computer")

    MenuPage {
        ScreenHeader("Settings", onBack = onBack, backLabel = "Menu")
        Text("These settings are just for $name.", style = MaterialTheme.typography.bodyLarge)

        PreviewBoard()

        SectionTitle("Board colours")
        // Four in a row normally; two by two when the text is large, so names aren't split.
        val perRow = if (LocalDensity.current.fontScale >= 1.3f) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in BoardTheme.entries.chunked(perRow)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    for (theme in row) {
                        ThemeSwatch(theme, selected = settings.boardTheme == theme, Modifier.weight(1f)) {
                            change(settings.copy(boardTheme = theme))
                        }
                    }
                }
            }
        }

        SectionTitle("Pieces and moves")
        SettingSwitch(
            "Large print", "A letter on each piece: K king, Q queen, R rook, B bishop, N knight",
            settings.largePrint,
        ) { change(settings.copy(largePrint = it)) }
        SettingSwitch(
            "Show possible moves", "Dots show where the chosen piece can go",
            settings.showLegalMoves,
        ) { change(settings.copy(showLegalMoves = it)) }
        SettingSwitch(
            "Ask before each move", "In games, confirm every move with Yes or No, so a slip of the finger doesn't count",
            settings.confirmMoves,
        ) { change(settings.copy(confirmMoves = it)) }
        SettingSwitch(
            "Always choose a queen", "A pawn reaching the far side becomes a queen without asking",
            settings.autoQueen,
        ) { change(settings.copy(autoQueen = it)) }

        SectionTitle("Sound and speech")
        SettingSwitch("Move sounds", "A soft tap when a piece moves", settings.sounds) {
            change(settings.copy(sounds = it), tryIt = it)
        }
        SettingSwitch("Vibrate", "A short buzz when a piece moves, so you notice the computer's move", settings.vibration) {
            change(settings.copy(vibration = it), tryIt = it)
        }
        SettingSwitch("Read moves aloud", "Says each move, for example \"Computer: knight to f3\"", settings.speakMoves) {
            change(settings.copy(speakMoves = it), tryIt = it)
        }
        Spacer(Modifier.heightIn(min = 8.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

/** A small board showing how the current choices look. */
@Composable
private fun PreviewBoard() {
    val game = remember { ChessGame().apply { listOf("e2e4", "e7e5", "g1f3", "b8c6").forEach(::makeUciMove) } }
    val selected = Square.parse("f1")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ChessBoard(
            pieces = game.pieces(),
            flipped = false,
            selected = selected,
            targets = game.legalMovesFrom(selected).map { it.to }.toSet(),
            lastMove = game.lastMove,
            checkSquare = null,
            animation = null,
            onSquareTap = {},
            onDragStart = { false },
            onDrop = { _, _ -> },
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f),
        )
    }
}

@Composable
private fun ThemeSwatch(theme: BoardTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = theme.palette()
    Card(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(
            if (selected) 4.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else Color(0xFFD6CDB8),
        ),
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val s = size.width / 2
                for (r in 0..1) for (c in 0..1) {
                    drawRect(if ((r + c) % 2 == 0) palette.lightSquare else palette.darkSquare, Offset(c * s, r * s), Size(s, s))
                }
            }
            Text(theme.label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

/** A whole-row switch: the large row is the touch target, not just the small switch. */
@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, Color(0xFFD6CDB8)),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4A4A4A))
            }
            Spacer(Modifier.width(12.dp))
            // The row handles the toggle; the switch only shows the state.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
