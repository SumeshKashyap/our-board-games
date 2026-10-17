package com.sumesh.ourchess.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.sumesh.ourchess.R
import com.sumesh.ourchess.data.PlayerSettings
import com.sumesh.ourchess.game.MoveEvent
import com.sumesh.ourchess.ui.theme.LocalPlayerSettings
import java.util.Locale

/**
 * Plays the player's chosen feedback (sound, vibration, speech) whenever a new move is made.
 * The move already on screen when this appears is not replayed.
 *
 * @param opponentName how the other side is named when its moves are read aloud.
 */
@Composable
fun MoveFeedback(event: MoveEvent?, opponentName: String) {
    val settings = LocalPlayerSettings.current
    val context = LocalContext.current
    val player = remember { FeedbackPlayer(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(settings.speakMoves) { if (settings.speakMoves) player.prepareSpeech() }

    val alreadyShown = remember { event?.id }
    LaunchedEffect(event?.id) {
        if (event != null && event.id != alreadyShown) player.play(event, settings, opponentName)
    }
}

class FeedbackPlayer(private val context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val moveSound = soundPool.load(context, R.raw.move, 1)
    private val captureSound = soundPool.load(context, R.raw.capture, 1)

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    /** Said as soon as the speech engine has started (the first time can take a moment). */
    private var pendingSpeech: String? = null

    fun prepareSpeech() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                tts?.setSpeechRate(0.9f)
                ttsReady = true
                pendingSpeech?.let(::speak)
                pendingSpeech = null
            }
        }
    }

    fun play(event: MoveEvent, settings: PlayerSettings, opponentName: String) {
        if (settings.sounds) {
            soundPool.play(if (event.capture) captureSound else moveSound, 1f, 1f, 1, 0, 1f)
        }
        if (settings.vibration) {
            // A firmer buzz for the opponent's move, so it is noticed even when looking away.
            vibrate(if (event.byOpponent) 70 else 25)
        }
        if (settings.speakMoves) {
            prepareSpeech()
            val text = if (event.byOpponent) "$opponentName: ${event.description}" else event.description
            if (ttsReady) speak(text) else pendingSpeech = text
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "move")
    }

    private fun vibrate(ms: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        soundPool.release()
        tts?.shutdown()
        tts = null
    }
}
