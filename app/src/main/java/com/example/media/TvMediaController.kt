package com.example.media

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent

data class MediaActionStatus(
    val actionName: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * TV Media Playback Controller using Android AudioManager.
 * Dispatches standard system media key events (NEXT, PREVIOUS, PLAY_PAUSE)
 * to background TV streaming applications (YouTube, Spotify, Netflix, VLC).
 */
class TvMediaController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    companion object {
        private const val TAG = "TvMediaController"
        private const val DEBOUNCE_INTERVAL_MS = 250L
    }

    private var lastEventTime: Long = 0L

    fun skipNext(): MediaActionStatus {
        if (!debounce()) {
            return MediaActionStatus("Debounced", "Command throttled")
        }
        Log.d(TAG, "Dispatching KEYCODE_MEDIA_NEXT")
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        return MediaActionStatus("NEXT TRACK", "Dispatched KEYCODE_MEDIA_NEXT")
    }

    fun skipPrevious(): MediaActionStatus {
        if (!debounce()) {
            return MediaActionStatus("Debounced", "Command throttled")
        }
        Log.d(TAG, "Dispatching KEYCODE_MEDIA_PREVIOUS")
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return MediaActionStatus("PREV TRACK", "Dispatched KEYCODE_MEDIA_PREVIOUS")
    }

    fun playPause(): MediaActionStatus {
        if (!debounce()) {
            return MediaActionStatus("Debounced", "Command throttled")
        }
        Log.d(TAG, "Dispatching KEYCODE_MEDIA_PLAY_PAUSE")
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        return MediaActionStatus("PLAY/PAUSE", "Dispatched KEYCODE_MEDIA_PLAY_PAUSE")
    }

    fun fastForward(): MediaActionStatus {
        if (!debounce()) {
            return MediaActionStatus("Debounced", "Command throttled")
        }
        Log.d(TAG, "Dispatching KEYCODE_MEDIA_FAST_FORWARD")
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        return MediaActionStatus("FAST FORWARD", "Dispatched KEYCODE_MEDIA_FAST_FORWARD")
    }

    fun rewind(): MediaActionStatus {
        if (!debounce()) {
            return MediaActionStatus("Debounced", "Command throttled")
        }
        Log.d(TAG, "Dispatching KEYCODE_MEDIA_REWIND")
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND)
        return MediaActionStatus("REWIND", "Dispatched KEYCODE_MEDIA_REWIND")
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val am = audioManager ?: return
        try {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(eventDown)
            am.dispatchMediaKeyEvent(eventUp)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching media key $keyCode", e)
        }
    }

    private fun debounce(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastEventTime < DEBOUNCE_INTERVAL_MS) {
            return false
        }
        lastEventTime = now
        return true
    }
}
