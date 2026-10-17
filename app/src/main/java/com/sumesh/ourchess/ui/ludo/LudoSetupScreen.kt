package com.sumesh.ourchess.ui.ludo

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.Profile
import com.sumesh.ourchess.ludo.LudoColor
import com.sumesh.ourchess.ludo.LudoSeat
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader

/** Who sits where: a seat is the computer, a family member, or a guest. */
private sealed interface SeatChoice {
    data object Computer : SeatChoice
    data object Guest : SeatChoice
    data class Player(val profile: Profile) : SeatChoice

    fun label(): String = when (this) {
        Computer -> "Computer"
        Guest -> "Guest"
        is Player -> profile.name
    }
}

@Composable
fun LudoSetupScreen(host: Profile, profiles: List<Profile>, onStart: (List<LudoSeat>) -> Unit, onBack: () -> Unit) {
    var count by remember { mutableIntStateOf(2) }
    // Choices for up to four seats; by default the current player against the computer.
    val choices = remember {
        mutableStateListOf<SeatChoice>(SeatChoice.Player(host), SeatChoice.Computer, SeatChoice.Computer, SeatChoice.Computer)
    }
    var editing by remember { mutableStateOf<Int?>(null) }
    val colors = LudoColor.forPlayers(count)

    MenuPage {
        ScreenHeader("New Ludo game", onBack = onBack, backLabel = "Back")
        Text("How many players?", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (n in 2..4) {
                val selected = n == count
                Card(
                    onClick = { count = n },
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFFD7E8D0) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFFD6CDB8)),
                ) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("$n", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        Text("Who is playing each colour?", style = MaterialTheme.typography.titleLarge)
        colors.forEachIndexed { i, color ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = color.light()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(40.dp).background(color.main(), CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(color.label, style = MaterialTheme.typography.bodyMedium)
                        Text(choices[i].label(), style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedButton(onClick = { editing = i }) { Text("Change") }
                }
            }
        }
        Text(
            "Family members take turns on this device: when it's your colour's turn, tap the dice.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4A4A4A),
        )
        Button(
            onClick = {
                onStart(colors.mapIndexed { i, color ->
                    when (val c = choices[i]) {
                        SeatChoice.Computer -> LudoSeat(color, "Computer", null, computer = true)
                        SeatChoice.Guest -> LudoSeat(color, "Guest", null, computer = false)
                        is SeatChoice.Player -> LudoSeat(color, c.profile.name, c.profile.id, computer = false)
                    }
                })
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        ) { Text("Start game", style = MaterialTheme.typography.labelLarge) }
    }

    editing?.let { seat ->
        // A family member can only sit in one seat.
        val taken = choices.take(count).filterIndexed { i, _ -> i != seat }.filterIsInstance<SeatChoice.Player>().map { it.profile.id }
        val options = listOf(SeatChoice.Computer) +
            profiles.filter { it.id !in taken }.map { SeatChoice.Player(it) } + SeatChoice.Guest
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("${colors[seat].label} is played by…", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (option in options) {
                        OutlinedButton(
                            onClick = { choices[seat] = option; editing = null },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) { Text(option.label(), style = MaterialTheme.typography.titleMedium) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
