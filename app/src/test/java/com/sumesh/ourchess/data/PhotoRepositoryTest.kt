package com.sumesh.ourchess.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repo = PhotoRepository(context)

    private fun picture(width: Int, height: Int): Uri {
        val file = File(context.cacheDir, "picked-${width}x$height.jpg")
        file.outputStream().use { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun addedPhotosAreResizedStoredAndRemovable() {
        val big = repo.add("asha", picture(4000, 3000))
        val small = repo.add("asha", picture(800, 600))
        assertNotNull(big); assertNotNull(small)

        val stored = BitmapFactory.decodeFile(big!!.path)
        assertTrue("longest side ${stored.width}", maxOf(stored.width, stored.height) <= 1600)
        assertEquals(2, repo.forProfile("asha").size)
        assertEquals(0, repo.forProfile("ravi").size)
        assertEquals(2, repo.all(listOf("asha", "ravi")).count { it is PhotoSource.Added })
        assertNotNull(repo.load(PhotoSource.Added(small!!), 400))

        repo.remove(small)
        assertEquals(listOf(big), repo.forProfile("asha"))
        repo.removeAll("asha")
        assertTrue(repo.forProfile("asha").isEmpty())
    }

    @Test
    fun pickRandomChoosesDistinctPhotos() {
        val pool = (1..5).toList()
        repeat(20) { seed ->
            val chosen = pickRandom(pool, 3, Random(seed))
            assertEquals(3, chosen.size)
            assertEquals(3, chosen.toSet().size)
        }
        assertEquals(setOf(1, 2), pickRandom(listOf(1, 2), 3).toSet())
        assertEquals(5, (0 until 200).flatMap { pickRandom(pool, 3, Random(it)) }.toSet().size) // every photo gets a turn
    }
}
