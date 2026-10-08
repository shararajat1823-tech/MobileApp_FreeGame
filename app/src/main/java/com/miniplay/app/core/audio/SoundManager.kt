package com.miniplay.app.core.audio

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.concurrent.atomic.AtomicBoolean

/** The set of feedback cues games can trigger. */
enum class SoundEffect { CLICK, CARD_FLIP, CORRECT, WRONG, WIN, GAME_OVER, ACHIEVEMENT }

interface SoundManager {
    fun play(effect: SoundEffect)
    fun setEnabled(enabled: Boolean)
    fun release()
}

/**
 * Lightweight sound engine built on the platform [ToneGenerator] so the app
 * ships zero audio assets (nothing to license). Each effect maps to a short
 * system tone. All calls are null-safe and guarded — audio failures must never
 * crash a game. Respects the user's "sound effects" setting via [setEnabled].
 */
class ToneSoundManager : SoundManager {

    private val enabled = AtomicBoolean(true)
    private val lock = Any()

    @Volatile
    private var generator: ToneGenerator? = null

    override fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
    }

    override fun play(effect: SoundEffect) {
        if (!enabled.get()) return
        val gen = obtain() ?: return
        val (tone, durationMs) = effect.toTone()
        runCatching { gen.startTone(tone, durationMs) }
    }

    override fun release() {
        synchronized(lock) {
            runCatching { generator?.release() }
            generator = null
        }
    }

    private fun obtain(): ToneGenerator? {
        generator?.let { return it }
        return synchronized(lock) {
            generator ?: runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
            }.getOrNull()?.also { generator = it }
        }
    }

    private fun SoundEffect.toTone(): Pair<Int, Int> = when (this) {
        SoundEffect.CLICK -> ToneGenerator.TONE_PROP_BEEP to 40
        SoundEffect.CARD_FLIP -> ToneGenerator.TONE_PROP_BEEP to 28
        SoundEffect.CORRECT -> ToneGenerator.TONE_PROP_ACK to 120
        SoundEffect.WRONG -> ToneGenerator.TONE_PROP_NACK to 150
        SoundEffect.WIN -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 350
        SoundEffect.GAME_OVER -> ToneGenerator.TONE_SUP_ERROR to 300
        SoundEffect.ACHIEVEMENT -> ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE to 300
    }

    private companion object {
        const val VOLUME = 70 // 0..100
    }
}
