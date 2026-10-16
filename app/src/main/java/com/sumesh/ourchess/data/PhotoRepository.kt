package com.sumesh.ourchess.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.random.Random

/** Where a photo comes from: bundled with the app, or added by a player on this device. */
sealed interface PhotoSource {
    data class Bundled(val assetPath: String) : PhotoSource
    data class Added(val file: File) : PhotoSource
}

/**
 * Photos for the start-up screen. Bundled family photos live in the APK's assets/loading/;
 * photos a player adds are copied (resized) into private storage under photos/<profile-id>/.
 */
class PhotoRepository(private val context: Context) {

    private val root = File(context.filesDir, "photos")

    fun bundled(): List<PhotoSource> =
        context.assets.list(ASSET_DIR)?.sorted()?.map { PhotoSource.Bundled("$ASSET_DIR/$it") }.orEmpty()

    fun forProfile(profileId: String): List<File> =
        File(root, profileId).listFiles { f -> f.isFile && f.extension == "jpg" }?.sortedBy { it.name }.orEmpty()

    /** Every photo available on this device: bundled ones plus every player's own. */
    fun all(profileIds: List<String>): List<PhotoSource> =
        bundled() + profileIds.flatMap { id -> forProfile(id).map { PhotoSource.Added(it) } }

    /** Copies a picked image into the player's photos, resized and rotated upright. Returns null if unreadable. */
    fun add(profileId: String, uri: Uri): File? = runCatching {
        val resolver = context.contentResolver
        val bitmap = decodeSampled(MAX_STORED) { resolver.openInputStream(uri)!! } ?: return null
        val orientation = resolver.openInputStream(uri)!!.use { ExifInterface(it).rotationDegrees }
        val upright = scaleDown(bitmap.rotate(orientation), MAX_STORED)
        val dir = File(root, profileId).apply { mkdirs() }
        // Time-ordered names keep photos in the order they were added.
        val file = File(dir, "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}.jpg")
        file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        file
    }.getOrNull()

    fun remove(file: File) {
        if (file.canonicalPath.startsWith(root.canonicalPath)) file.delete()
    }

    fun removeAll(profileId: String) {
        File(root, profileId).deleteRecursively()
    }

    fun load(source: PhotoSource, maxSize: Int): Bitmap? = runCatching {
        when (source) {
            is PhotoSource.Bundled -> decodeSampled(maxSize) { context.assets.open(source.assetPath) }
            is PhotoSource.Added -> decodeSampled(maxSize) { source.file.inputStream() }
        }
    }.getOrNull()

    companion object {
        const val ASSET_DIR = "loading"
        private const val MAX_STORED = 1600
    }
}

/** Picks [count] different photos at random (all of them if there are fewer). */
fun <T> pickRandom(pool: List<T>, count: Int, random: Random = Random.Default): List<T> = pool.shuffled(random).take(count)

/** Decodes an image at roughly [maxSize] pixels on its longest side, to save memory. */
private fun decodeSampled(maxSize: Int, open: () -> InputStream): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null
    var sample = 1
    while (longest / (sample * 2) >= maxSize) sample *= 2
    return open().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
}

private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= maxSize) return bitmap
    val scale = maxSize.toFloat() / longest
    return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
}

private fun Bitmap.rotate(degrees: Int): Bitmap =
    if (degrees == 0) this else Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
