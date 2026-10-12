package com.example.volume

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log

data class VolumeStatus(
    val currentVolume: Int,
    val maxVolume: Int,
    val percentage: Int,
    val isFixed: Boolean,
    val isMuted: Boolean,
    val lastActionMessage: String
)

class TvVolumeController(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var lastActionTime = 0L
    private val debounceThresholdMs = 80L

    companion object {
        private const val TAG = "TvVolumeController"
    }

    /**
     * Inspects current TV volume state on STREAM_MUSIC (fallback to STREAM_SYSTEM)
     */
    fun getStatus(): VolumeStatus {
        if (audioManager == null) {
            return VolumeStatus(
                currentVolume = 0,
                maxVolume = 100,
                percentage = 0,
                isFixed = false,
                isMuted = false,
                lastActionMessage = "AudioManager service is unavailable on this TV firmware."
            )
        }

        val isFixed = try {
            audioManager.isVolumeFixed
        } catch (e: Exception) {
            false
        }

        val stream = getActiveStream()
        val current = try {
            audioManager.getStreamVolume(stream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to getStreamVolume", e)
            0
        }

        val max = try {
            val m = audioManager.getStreamMaxVolume(stream)
            if (m <= 0) 1 else m
        } catch (e: Exception) {
            1
        }

        val pct = ((current.toFloat() / max.toFloat()) * 100).toInt().coerceIn(0, 100)

        val isMuted = try {
            audioManager.isStreamMute(stream)
        } catch (e: Exception) {
            current == 0
        }

        val msg = if (isFixed) {
            "Warning: This TV audio output is set to fixed volume (e.g. external optical/HDMI-ARC). Adjust audio via TV sound settings."
        } else {
            "Volume: $pct% ($current/$max)"
        }

        return VolumeStatus(
            currentVolume = current,
            maxVolume = max,
            percentage = pct,
            isFixed = isFixed,
            isMuted = isMuted,
            lastActionMessage = msg
        )
    }

    /**
     * Increases actual TV volume with AudioManager.ADJUST_RAISE
     */
    fun volumeUp(): VolumeStatus {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < debounceThresholdMs) {
            return getStatus()
        }
        lastActionTime = now

        if (audioManager == null) {
            return getStatus().copy(lastActionMessage = "Error: AudioManager unavailable on device.")
        }

        if (audioManager.isVolumeFixed) {
            return getStatus().copy(lastActionMessage = "TV Audio is Fixed: Volume changes disabled by TV firmware.")
        }

        val stream = getActiveStream()
        try {
            audioManager.adjustStreamVolume(
                stream,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException adjusting volume up", e)
            return getStatus().copy(lastActionMessage = "TV Permission Error: Volume control restricted by firmware.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception adjusting volume up", e)
            return getStatus().copy(lastActionMessage = "Error adjusting volume: ${e.message}")
        }

        val status = getStatus()
        return status.copy(lastActionMessage = "Volume Increased: ${status.percentage}%")
    }

    /**
     * Decreases actual TV volume with AudioManager.ADJUST_LOWER
     */
    fun volumeDown(): VolumeStatus {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < debounceThresholdMs) {
            return getStatus()
        }
        lastActionTime = now

        if (audioManager == null) {
            return getStatus().copy(lastActionMessage = "Error: AudioManager unavailable on device.")
        }

        if (audioManager.isVolumeFixed) {
            return getStatus().copy(lastActionMessage = "TV Audio is Fixed: Volume changes disabled by TV firmware.")
        }

        val stream = getActiveStream()
        try {
            audioManager.adjustStreamVolume(
                stream,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException adjusting volume down", e)
            return getStatus().copy(lastActionMessage = "TV Permission Error: Volume control restricted by firmware.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception adjusting volume down", e)
            return getStatus().copy(lastActionMessage = "Error adjusting volume: ${e.message}")
        }

        val status = getStatus()
        return status.copy(lastActionMessage = "Volume Decreased: ${status.percentage}%")
    }

    /**
     * Toggles Mute state
     */
    fun toggleMute(): VolumeStatus {
        if (audioManager == null) return getStatus()
        val stream = getActiveStream()
        try {
            audioManager.adjustStreamVolume(
                stream,
                AudioManager.ADJUST_TOGGLE_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            Log.e(TAG, "Mute toggle failed", e)
        }
        return getStatus()
    }

    private fun getActiveStream(): Int {
        return AudioManager.STREAM_MUSIC
    }
}
