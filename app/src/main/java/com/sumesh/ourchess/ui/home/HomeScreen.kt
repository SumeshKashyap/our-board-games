@file:OptIn(ExperimentalLayoutApi::class)

package com.sumesh.ourchess.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.checkers.CheckersColor
import com.sumesh.ourchess.checkers.CheckersPiece
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.data.Profile
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.checkers.PieceDisc
import com.sumesh.ourchess.ui.common.ChoiceCard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.PieceIcon
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.ludo.DiceFace

/** The player's home: choose a game, or look after photos, statistics and settings. */
@Composable
fun HomeScreen(
    profile: Profile,
    photoCount: Int,
    onChess: () -> Unit,
    onCheckers: () -> Unit,
    onLudo: () -> Unit,
    onStats: () -> Unit,
    onPhotos: () -> Unit,
    onSettings: () -> Unit,
    onChangeLevel: () -> Unit,
    onSwitchPlayer: () -> Unit,
) {
    MenuPage {
        Text("Hello, ${profile.name}", style = MaterialTheme.typography.headlineMedium)
        // Wraps the button onto its own line when the text is large.
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Level ${profile.level} · ${Levels.name(profile.level)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            OutlinedButton(onClick = onChangeLevel) { Text("Change level") }
        }
        Text("What would you like to play?", style = MaterialTheme.typography.titleLarge)

        ChoiceCard("Chess", "Play the computer, solve puzzles, learn moves", onChess, leading = {
            PieceIcon(Side.WHITE, PieceType.KING)
        })
        ChoiceCard("Checkers", "Play the computer, solve puzzles, learn the rules", onCheckers, leading = {
            PieceDisc(CheckersPiece(CheckersColor.BLACK, king = true), 56.dp)
        })
        ChoiceCard("Ludo", "2 to 4 players: family and computer", onLudo, leading = { DiceFace(5, 52.dp) })

        Text("More", style = MaterialTheme.typography.titleLarge)
        ChoiceCard("My statistics", "Games won, drawn and lost", onStats, leading = { MenuIcon(Icons.Filled.Star) })
        ChoiceCard(
            "My photos",
            if (photoCount > 0) "$photoCount photos · shown when the app starts" else "Add pictures to show when the app starts",
            onPhotos,
            leading = { MenuIcon(Icons.Filled.Face) },
        )
        ChoiceCard("Settings", "Board colours, large print, sounds and more", onSettings, leading = { MenuIcon(Icons.Filled.Settings) })
        OutlinedButton(onClick = onSwitchPlayer, modifier = Modifier.fillMaxWidth()) {
            Text("Switch player", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MenuIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
}

/** One choice in a game's own menu. */
data class GameMenuItem(val title: String, val subtitle: String, val onClick: () -> Unit)

/** A game's menu, e.g. Chess: play / puzzles / learn. */
@Composable
fun GameMenuScreen(title: String, icon: @Composable () -> Unit, items: List<GameMenuItem>, onBack: () -> Unit) {
    MenuPage {
        ScreenHeader(title, onBack = onBack, backLabel = "Home")
        for (item in items) ChoiceCard(item.title, item.subtitle, item.onClick, leading = icon)
    }
}
