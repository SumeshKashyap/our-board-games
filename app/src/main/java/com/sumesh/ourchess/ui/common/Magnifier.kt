package com.sumesh.ourchess.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumesh.ourchess.data.ZOOM_STEPS
import kotlin.math.roundToInt

/**
 * The magnifying glass: a round button that opens + / − controls to make everything on screen bigger or smaller.
 */
@Composable
fun Magnifier(zoom: Float, onZoom: (Float) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val bigger = ZOOM_STEPS.firstOrNull { it > zoom + 0.001f }
    val smaller = ZOOM_STEPS.lastOrNull { it < zoom - 0.001f }

    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { open = !open },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp,
            modifier = Modifier.size(56.dp).semantics { contentDescription = if (open) "Close zoom" else "Zoom" },
        ) {
            Icon(
                if (open) Icons.Filled.Close else Icons.Filled.Search,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(14.dp),
            )
        }
        if (open) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ZoomButton("+", "Zoom in", enabled = bigger != null) { bigger?.let(onZoom) }
                    Text("${(zoom * 100).roundToInt()}%", fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
                    ZoomButton("−", "Zoom out", enabled = smaller != null) { smaller?.let(onZoom) }
                    TextButton(onClick = { onZoom(1f) }, enabled = zoom != 1f) { Text("Reset", fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun ZoomButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(64.dp).semantics { contentDescription = description },
    ) {
        Text(symbol, fontSize = 32.sp)
    }
}
