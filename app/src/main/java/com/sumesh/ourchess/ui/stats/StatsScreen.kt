package com.sumesh.ourchess.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.GameKind
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.data.Record
import com.sumesh.ourchess.data.Stats
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader

@Composable
fun StatsScreen(name: String, stats: Stats, onBack: () -> Unit) {
    MenuPage {
        ScreenHeader("My statistics", onBack = onBack, backLabel = "Home")
        Text("$name's results against the computer and family.", style = MaterialTheme.typography.bodyLarge)
        for (game in GameKind.entries) {
            val total = stats.total(game)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, Color(0xFFD6CDB8)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(game.label, style = MaterialTheme.typography.titleLarge)
                    if (total.played == 0) {
                        Text("No finished games yet.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4A4A4A))
                    } else {
                        Row(Modifier.fillMaxWidth()) {
                            Figure("Played", total.played, Modifier.weight(1f))
                            Figure("Won", total.won, Modifier.weight(1f))
                            if (game != GameKind.LUDO) Figure("Drawn", total.drawn, Modifier.weight(1f))
                            Figure("Lost", total.lost, Modifier.weight(1f))
                        }
                        for (level in 1..5) {
                            val r: Record = stats.record(game, level)
                            if (r.played == 0) continue
                            Text(
                                "Level $level · ${Levels.name(level)}: won ${r.won} of ${r.played}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: Int, modifier: Modifier) {
    Column(modifier) {
        Text("$value", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
