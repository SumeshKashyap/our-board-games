package com.sumesh.ourchess.ui.photos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.data.PhotoRepository
import com.sumesh.ourchess.data.PhotoSource
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A player's own photos, which join the family photos on the start-up screen. */
@Composable
fun PhotosScreen(
    photos: List<File>,
    repository: PhotoRepository,
    adding: Boolean,
    onAdd: (List<Uri>) -> Unit,
    onRemove: (File) -> Unit,
    onBack: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)) { uris ->
        if (uris.isNotEmpty()) onAdd(uris)
    }
    var toRemove by remember { mutableStateOf<File?>(null) }

    MenuPage {
        ScreenHeader("My photos", onBack = onBack, backLabel = "Menu")
        Text(
            "Your photos appear when Our Chess starts, together with the family's. They stay on this device.",
            style = MaterialTheme.typography.bodyLarge,
        )
        BigButton(
            if (adding) "Adding photos…" else "Add photos",
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Add,
            enabled = !adding,
        )
        if (photos.isEmpty()) {
            Text("No photos yet.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4A4A4A))
        } else {
            BoxWithConstraints {
                val columns = if (maxWidth > 500.dp) 3 else 2
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in photos.chunked(columns)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (file in row) {
                                PhotoTile(file, repository, onRemove = { toRemove = file }, Modifier.weight(1f))
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }

    toRemove?.let { file ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text("Remove this photo?") },
            text = { Text("It will no longer appear when the app starts.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { onRemove(file); toRemove = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PhotoTile(file: File, repository: PhotoRepository, onRemove: () -> Unit, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { repository.load(PhotoSource.Added(file), 500)?.asImageBitmap() }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE4DCC8)),
        ) {
            bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) }
        }
        OutlinedButton(onClick = onRemove) { Text("Remove") }
    }
}
