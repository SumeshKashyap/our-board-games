package com.sumesh.ourchess.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.data.Profile
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.ChoiceCard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.PieceIcon

@Composable
fun WelcomeScreen(
    profiles: List<Profile>,
    onCreate: (String) -> Unit,
    onChoose: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
) {
    // With no players yet, go straight to asking for a name.
    var creating by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Profile?>(null) }
    val showForm = creating || profiles.isEmpty()

    // Transparent, so the start-up photos show softly behind it.
    MenuPage(background = Color.Transparent) {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PieceIcon(Side.WHITE, PieceType.KING, 88.dp)
            Spacer(Modifier.height(8.dp))
            Text("Welcome to Our Board Games", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                if (showForm) "Let's get you started." else "Who is playing today?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4A4A4A),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (showForm) {
            NewProfileForm(
                onCreate = onCreate,
                onCancel = if (profiles.isNotEmpty()) ({ creating = false }) else null,
            )
        } else {
            for (profile in profiles) {
                ChoiceCard(
                    title = profile.name,
                    subtitle = "Level ${profile.level} · ${Levels.name(profile.level)}",
                    onClick = { onChoose(profile) },
                    leading = { Initial(profile.name) },
                    trailing = { TextButton(onClick = { toDelete = profile }) { Text("Remove") } },
                )
            }
            BigButton("New player", onClick = { creating = true }, icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth())
        }
    }

    toDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Remove ${profile.name}?") },
            text = { Text("Their progress will be deleted.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { onDelete(profile); toDelete = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NewProfileForm(onCreate: (String) -> Unit, onCancel: (() -> Unit)?) {
    var name by remember { mutableStateOf("") }
    val valid = name.isNotBlank()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("What is your name?", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
            placeholder = { Text("Your name", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) onCreate(name) }),
            // Solid background so the photos behind this screen don't show through the text.
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onCreate(name) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        ) { Text("Continue", style = MaterialTheme.typography.labelLarge) }
        if (onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun Initial(name: String) {
    Box(
        Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
