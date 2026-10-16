package com.sumesh.ourchess.ui.level

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.Levels
import com.sumesh.ourchess.ui.common.ChoiceCard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader

@Composable
fun LevelScreen(name: String, current: Int, onChoose: (Int) -> Unit, onBack: () -> Unit) {
    MenuPage {
        ScreenHeader(title = "Hello, $name!", onBack = onBack)
        Text("Choose a difficulty level", style = MaterialTheme.typography.headlineSmall)
        for (level in Levels.all) {
            ChoiceCard(
                title = "Level $level · ${Levels.name(level)}",
                subtitle = Levels.description(level),
                onClick = { onChoose(level) },
                highlighted = level == current,
                leading = { LevelBadge(level, level == current) },
            )
        }
    }
}

@Composable
private fun LevelBadge(level: Int, selected: Boolean) {
    Box(
        Modifier
            .size(56.dp)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE4DCC8), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            level.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (selected) Color.White else Color(0xFF1F1F1F),
        )
    }
}
