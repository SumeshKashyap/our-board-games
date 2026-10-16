package com.sumesh.ourchess.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.data.Profile
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.common.ChoiceCard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.PieceIcon
import com.sumesh.ourchess.tutorials.Lessons

@Composable
fun MenuScreen(
    profile: Profile,
    hasGameInProgress: Boolean,
    onPlay: () -> Unit,
    onPuzzles: () -> Unit,
    onLessons: () -> Unit,
    onChangeLevel: () -> Unit,
    onSwitchPlayer: () -> Unit,
) {
    MenuPage {
        Text("Hello, ${profile.name}", style = MaterialTheme.typography.headlineMedium)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Level ${profile.level} · ${Levels.name(profile.level)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onChangeLevel) { Text("Change level") }
        }
        Text("What would you like to do?", style = MaterialTheme.typography.bodyLarge)

        ChoiceCard(
            title = "Play against computer",
            subtitle = if (hasGameInProgress) "Continue your game" else "A full game at your level",
            onClick = onPlay,
            leading = { PieceIcon(Side.WHITE, PieceType.KNIGHT) },
        )
        ChoiceCard(
            title = "Puzzles",
            subtitle = if (profile.puzzlesSolved > 0) "${profile.puzzlesSolved} solved so far" else "Find the winning move",
            onClick = onPuzzles,
            leading = { PieceIcon(Side.BLACK, PieceType.QUEEN) },
        )
        ChoiceCard(
            title = "Learn moves",
            subtitle = "${profile.lessonsDone.size} of ${Lessons.all.size} lessons done",
            onClick = onLessons,
            leading = { PieceIcon(Side.WHITE, PieceType.PAWN) },
        )
        OutlinedButton(onClick = onSwitchPlayer, modifier = Modifier.fillMaxWidth()) {
            Text("Switch player", style = MaterialTheme.typography.labelLarge)
        }
    }
}
