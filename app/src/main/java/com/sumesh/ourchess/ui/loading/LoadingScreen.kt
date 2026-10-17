package com.sumesh.ourchess.ui.loading

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.common.PieceIcon
import kotlinx.coroutines.delay

/** How long the photos are shown at full brightness before moving on. */
private const val SHOW_FOR_MS = 5_000L
private const val FADE_MS = 2_000
private const val SLIDE_MS = 1_700L

/** Faces are usually in the upper part of a photo, so crop from slightly above the centre. */
private val PhotoCrop = BiasAlignment(0f, -0.35f)

/**
 * The start-up photos. Bright while loading; once [dimmed], they slowly fade into a soft background
 * for the screen on top (choosing a player).
 * Landscape shows the photos side by side; portrait shows them one after another.
 */
@Composable
fun PhotoBackdrop(photos: List<ImageBitmap>, dimmed: Boolean) {
    if (photos.isEmpty()) return
    val alpha by animateFloatAsState(if (dimmed) 0.2f else 1f, tween(FADE_MS), label = "backdrop")
    BoxWithConstraints(Modifier.fillMaxSize().alpha(alpha)) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize()) {
                for (photo in photos) {
                    Image(photo, null, Modifier.weight(1f).fillMaxHeight(), PhotoCrop, ContentScale.Crop)
                }
            }
        } else {
            var index by remember { mutableIntStateOf(0) }
            LaunchedEffect(photos.size) {
                while (photos.size > 1) {
                    delay(SLIDE_MS)
                    index = (index + 1) % photos.size
                }
            }
            Crossfade(index, animationSpec = tween(700), label = "slideshow") { i ->
                Image(photos[i % photos.size], null, Modifier.fillMaxSize(), PhotoCrop, ContentScale.Crop)
            }
        }
    }
}

/**
 * The title shown over the photos while the app gets ready. Moves on after a few seconds,
 * or straight away when tapped (once ready).
 */
@Composable
fun LoadingOverlay(hasPhotos: Boolean, isReady: Boolean, onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    var minTimePassed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SHOW_FOR_MS)
        minTimePassed = true
    }
    LaunchedEffect(minTimePassed, isReady) { if (minTimePassed && isReady) done() }

    Box(
        Modifier
            .fillMaxSize()
            .then(if (hasPhotos) Modifier else Modifier.background(MaterialTheme.colorScheme.primary))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (isReady) done()
            },
    ) {
        if (hasPhotos) {
            // Darken the lower part so the title is readable over any photo.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xCC000000))))
        }
        Column(
            Modifier
                .align(if (hasPhotos) Alignment.BottomCenter else Alignment.Center)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasPhotos) PieceIcon(Side.WHITE, PieceType.KING, 120.dp)
            Text("Our Board Games", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            if (minTimePassed && !isReady) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    Text("Getting ready…", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text("A game for the whole family", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
