package com.miniplay.app.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.atomic.AtomicBoolean

/** Semantic haptic cues. The manager maps each to an appropriate effect per API level. */
enum class HapticFeedbackType { TICK, LIGHT, MEDIUM, HEAVY, SUCCESS, ERROR }

interface HapticManager {
    fun perform(type: HapticFeedbackType)
    fun setEnabled(enabled: Boolean)
}

/**
 * Vibration feedback that degrades gracefully across API levels: predefined
 * effects / primitives on newer devices, simple one-shots on older ones, and a
 * silent no-op where there is no vibrator. Respects the user's haptics setting.
 */
class SystemHapticManager(context: Context) : HapticManager {

    private val appContext = context.applicationContext
    private val enabled = AtomicBoolean(true)

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()?.takeIf { it.hasVibrator() }
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
    }

    override fun perform(type: HapticFeedbackType) {
        if (!enabled.get()) return
        val vib = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(type.toEffect())
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(type.legacyDurationMs())
            }
        }
    }

    private fun HapticFeedbackType.toEffect(): VibrationEffect = when (this) {
        HapticFeedbackType.TICK ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(10, 60)
            }
        HapticFeedbackType.LIGHT -> VibrationEffect.createOneShot(15, 90)
        HapticFeedbackType.MEDIUM ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            } else {
                VibrationEffect.createOneShot(25, 140)
            }
        HapticFeedbackType.HEAVY ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createOneShot(40, 200)
            }
        HapticFeedbackType.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 30), -1)
        HapticFeedbackType.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40), -1)
    }

    private fun HapticFeedbackType.legacyDurationMs(): Long = when (this) {
        HapticFeedbackType.TICK -> 10
        HapticFeedbackType.LIGHT -> 15
        HapticFeedbackType.MEDIUM -> 25
        HapticFeedbackType.HEAVY -> 40
        HapticFeedbackType.SUCCESS -> 50
        HapticFeedbackType.ERROR -> 80
    }
}
