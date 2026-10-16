package com.sumesh.ourchess.ui.loading

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.common.PieceIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PHOTO_DIR = "loading"
private const val SHOW_FOR_MS = 4_000L
private const val SLIDE_MS = 2_000L

/**
 * The opening screen: family photos (if bundled) behind the app name while the app gets ready.
 * Moves on by itself after a few seconds, or straight away when tapped.
 */
@Composable
fun LoadingScreen(isReady: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    LaunchedEffect(Unit) { photos = withContext(Dispatchers.IO) { loadPhotos(context) } }

    val done by rememberUpdatedState(onDone)
    var minTimePassed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SHOW_FOR_MS)
        minTimePassed = true
    }
    LaunchedEffect(minTimePassed, isReady) { if (minTimePassed && isReady) done() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (isReady) done()
            },
    ) {
        val loaded = photos.orEmpty()
        if (loaded.isNotEmpty()) {
            // Landscape fits several portrait photos side by side; portrait shows one at a time.
            val perSlide = if (maxWidth > maxHeight) minOf(3, loaded.size) else 1
            Slideshow(loaded, perSlide)
        }

        // Darken the lower part so the title is readable over any photo.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xCC000000))),
        )
        Column(
            Modifier
                .align(if (loaded.isEmpty()) Alignment.Center else Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loaded.isEmpty()) PieceIcon(Side.WHITE, PieceType.KING, 120.dp)
            Text("Our Chess", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun Slideshow(photos: List<ImageBitmap>, perSlide: Int) {
    var start by remember { mutableIntStateOf(0) }
    LaunchedEffect(photos.size, perSlide) {
        if (photos.size <= perSlide) return@LaunchedEffect
        while (true) {
            delay(SLIDE_MS)
            start = (start + 1) % photos.size
        }
    }
    Crossfade(targetState = start, animationSpec = tween(700), label = "slideshow") { first ->
        Row(Modifier.fillMaxSize()) {
            repeat(perSlide) { i ->
                Image(
                    bitmap = photos[(first + i) % photos.size],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

/** Decodes the bundled photos, scaled down to roughly screen size to save memory. */
private fun loadPhotos(context: Context): List<ImageBitmap> {
    val names = context.assets.list(PHOTO_DIR)?.sorted().orEmpty()
    return names.mapNotNull { name ->
        runCatching {
            val path = "$PHOTO_DIR/$name"
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1000) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
        }.getOrNull()
    }
}
