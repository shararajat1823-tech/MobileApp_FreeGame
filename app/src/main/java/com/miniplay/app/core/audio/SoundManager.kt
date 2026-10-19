package com.miniplay.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.sin

/** The set of feedback cues games can trigger. */
enum class SoundEffect { CLICK, CARD_FLIP, CORRECT, WRONG, WIN, GAME_OVER, ACHIEVEMENT, DICE_ROLL }

interface SoundManager {
    fun play(effect: SoundEffect)

    /**
     * A soft, pleasant merge chime whose pitch rises with the merged tile
     * [value] (e.g. for 2048) — satisfying and "attractive" without an asset.
     * Default no-op so non-audio implementations need not override it.
     */
    fun playMerge(value: Int) {}

    /** A short soft "tap" for a game piece hopping one cell (e.g. Ludo tokens). */
    fun playMove() {}

    fun setEnabled(enabled: Boolean)
    fun release()
}

/**
 * Lightweight sound engine. Most cues are short system tones via [ToneGenerator]
 * (zero assets to license). [SoundEffect.DICE_ROLL] is a richer, original dice
 * "rattle" synthesized at runtime into PCM and played via [AudioTrack] — a few
 * decaying noise knocks that sound like tumbling dice, instead of a beep. All
 * calls are guarded so audio failures never crash a game, and everything
 * respects the user's "sound effects" setting via [setEnabled].
 */
class ToneSoundManager : SoundManager {

    private val enabled = AtomicBoolean(true)
    private val lock = Any()

    @Volatile
    private var generator: ToneGenerator? = null

    @Volatile
    private var diceTrack: AudioTrack? = null

    @Volatile
    private var mergeTrack: AudioTrack? = null

    @Volatile
    private var moveTrack: AudioTrack? = null

    private val diceBuffer: ShortArray by lazy { buildDiceRattle() }
    private val mergeBuffer: ShortArray by lazy { buildMergeChime() }
    private val moveBuffer: ShortArray by lazy { buildMoveTick() }

    override fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
    }

    override fun play(effect: SoundEffect) {
        if (!enabled.get()) return
        if (effect == SoundEffect.DICE_ROLL) {
            playDiceRattle()
            return
        }
        val gen = obtain() ?: return
        val (tone, durationMs) = effect.toTone()
        runCatching { gen.startTone(tone, durationMs) }
    }

    override fun playMerge(value: Int) {
        if (!enabled.get()) return
        val track = obtainMergeTrack() ?: return
        runCatching {
            // Each doubling raises the pitch two semitones, capped at two octaves.
            val steps = (log2(value.coerceAtLeast(2).toDouble()).toInt() - 1).coerceAtLeast(0)
            val semitones = (steps * 2).coerceAtMost(24)
            val mult = Math.pow(2.0, semitones / 12.0)
            val rate = (SAMPLE_RATE * mult).toInt().coerceIn(8000, 48000)
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.playbackRate = rate
            track.play()
        }
    }

    override fun playMove() {
        if (!enabled.get()) return
        val track = obtainMoveTrack() ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    override fun release() {
        synchronized(lock) {
            runCatching { generator?.release() }
            generator = null
            runCatching { diceTrack?.release() }
            diceTrack = null
            runCatching { mergeTrack?.release() }
            mergeTrack = null
            runCatching { moveTrack?.release() }
            moveTrack = null
        }
    }

    // ------------------------------ tones ------------------------------

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
        SoundEffect.DICE_ROLL -> ToneGenerator.TONE_PROP_BEEP to 40 // unused: handled via AudioTrack
    }

    // ------------------------------ dice rattle ------------------------------

    private fun playDiceRattle() {
        val track = obtainDiceTrack() ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    private fun obtainDiceTrack(): AudioTrack? {
        diceTrack?.let { return it }
        return synchronized(lock) {
            diceTrack ?: runCatching { createDiceTrack() }.getOrNull()?.also { diceTrack = it }
        }
    }

    private fun createDiceTrack(): AudioTrack = createStaticTrack(diceBuffer)

    /** Synthesizes ~0.5s of dice-knock noise: several decaying noise bursts. */
    private fun buildDiceRattle(): ShortArray {
        val durMs = 520
        val n = SAMPLE_RATE * durMs / 1000
        val out = FloatArray(n)
        val rnd = java.util.Random(20260413L)
        // Each knock is a short, sharp, exponentially-decaying noise burst.
        val knockTimes = floatArrayOf(0.02f, 0.10f, 0.17f, 0.25f, 0.34f, 0.44f, 0.55f, 0.67f, 0.80f, 0.92f)
        for (k in knockTimes.indices) {
            val start = (knockTimes[k] * n).toInt()
            val burst = (SAMPLE_RATE * (0.016 + rnd.nextDouble() * 0.012)).toInt().coerceAtLeast(1)
            val amp = (0.5f + rnd.nextFloat() * 0.5f) * (1f - k.toFloat() / knockTimes.size * 0.35f)
            for (i in 0 until burst) {
                val idx = start + i
                if (idx >= n) break
                val env = exp(-7.0 * i / burst).toFloat()
                val noise = rnd.nextFloat() * 2f - 1f
                out[idx] += noise * env * amp
            }
        }
        // Short fade-out tail to avoid a click at the end.
        val fade = (SAMPLE_RATE * 0.04).toInt().coerceAtLeast(1)
        for (i in 0 until fade) {
            val idx = n - 1 - i
            if (idx >= 0) out[idx] *= i.toFloat() / fade
        }
        // Normalize and convert to 16-bit PCM.
        var max = 0f
        for (v in out) if (abs(v) > max) max = abs(v)
        val gain = if (max > 0f) 0.9f / max else 1f
        return ShortArray(n) { (out[it] * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort() }
    }

    // ------------------------------ merge chime ------------------------------

    private fun obtainMergeTrack(): AudioTrack? {
        mergeTrack?.let { return it }
        return synchronized(lock) {
            mergeTrack ?: runCatching { createStaticTrack(mergeBuffer) }.getOrNull()?.also { mergeTrack = it }
        }
    }

    /** A soft two-harmonic "pluck" with a quick attack and exponential decay. */
    private fun buildMergeChime(): ShortArray {
        val durMs = 260
        val n = SAMPLE_RATE * durMs / 1000
        val out = FloatArray(n)
        val f0 = 523.25 // C5
        val twoPi = 2.0 * Math.PI
        val durSec = durMs / 1000.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-5.0 * t / durSec)
            val attack = (t / 0.004).coerceAtMost(1.0) // 4ms fade-in
            val s = sin(twoPi * f0 * t) * 0.6 + sin(twoPi * 2 * f0 * t) * 0.25 + sin(twoPi * 3 * f0 * t) * 0.12
            out[i] = (s * env * attack).toFloat()
        }
        var max = 0f
        for (v in out) if (abs(v) > max) max = abs(v)
        val gain = if (max > 0f) 0.85f / max else 1f
        return ShortArray(n) { (out[it] * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort() }
    }

    // ------------------------------ move tick ------------------------------

    private fun obtainMoveTrack(): AudioTrack? {
        moveTrack?.let { return it }
        return synchronized(lock) {
            moveTrack ?: runCatching { createStaticTrack(moveBuffer) }.getOrNull()?.also { moveTrack = it }
        }
    }

    /** A short, soft "tok" — a low sine with a snappy noise transient. */
    private fun buildMoveTick(): ShortArray {
        val durMs = 60
        val n = SAMPLE_RATE * durMs / 1000
        val out = FloatArray(n)
        val rnd = java.util.Random(99L)
        val f0 = 200.0
        val twoPi = 2.0 * Math.PI
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val toneEnv = exp(-24.0 * t)
            val noiseEnv = exp(-90.0 * t)
            val tone = sin(twoPi * f0 * t) * 0.6 * toneEnv
            val noise = (rnd.nextFloat() * 2f - 1f) * 0.4f * noiseEnv.toFloat()
            out[i] = (tone.toFloat() + noise)
        }
        var max = 0f
        for (v in out) if (abs(v) > max) max = abs(v)
        val gain = if (max > 0f) 0.7f / max else 1f
        return ShortArray(n) { (out[it] * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort() }
    }

    private fun createStaticTrack(buf: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buf, 0, buf.size)
        return track
    }

    private companion object {
        const val VOLUME = 70 // 0..100
        const val SAMPLE_RATE = 22050
    }
}
