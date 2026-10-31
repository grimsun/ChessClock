package com.lab.chessclock.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibrations that tell players what the clock did without them looking at it. */
interface Haptics {
    /** A player ended their turn and the opponent's clock started. */
    fun turnEnded()

    /** For devices and tests without a vibrator. */
    object None : Haptics {
        override fun turnEnded() {}
    }
}

/** [Haptics] played on the device's vibrator, if it has one. */
class VibratorHaptics(context: Context) : Haptics {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    // A crisp click, like a physical clock's button.
    private val turnEnded =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        }

    override fun turnEnded() = vibrate(turnEnded)

    private fun vibrate(effect: VibrationEffect) {
        vibrator?.takeIf { it.hasVibrator() }?.vibrate(effect)
    }
}
