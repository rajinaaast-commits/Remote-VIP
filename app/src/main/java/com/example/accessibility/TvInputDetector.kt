package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.overlay.AuraOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State representing current TV input field detection.
 */
data class InputDetectionState(
    val isInputActive: Boolean = false,
    val lastPackageName: String = "None",
    val lastClassName: String = "None",
    val lastDetectionTime: Long = 0L,
    val totalDetections: Int = 0
)

/**
 * Component responsible for detecting when an input or search field is active
 * across running TV applications and automatically triggering the custom floating keyboard.
 */
object TvInputDetector {

    private const val TAG = "TvInputDetector"
    private var lastTriggerTime: Long = 0L
    private const val DEBOUNCE_MS = 1200L

    private val _detectionState = MutableStateFlow(InputDetectionState())
    val detectionState: StateFlow<InputDetectionState> = _detectionState.asStateFlow()

    /**
     * Inspects accessibility events dispatched to AuraAccessibilityService.
     * When an active editable input field is detected on the TV screen,
     * triggers the display of the floating keyboard.
     */
    fun onAccessibilityEvent(service: AccessibilityService, event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        // Only evaluate relevant focus, click, or text events
        if (eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val pkgName = event.packageName?.toString() ?: ""
        // Ignore events originating from this app's own internal UI
        if (pkgName == service.packageName && !isSimulatedPackage(pkgName)) {
            return
        }

        val isEditableField = checkIsEditable(event)
        if (!isEditableField) return

        val now = SystemClock.uptimeMillis()
        if (now - lastTriggerTime < DEBOUNCE_MS) {
            return
        }
        lastTriggerTime = now

        val className = event.className?.toString() ?: "android.widget.EditText"
        Log.i(TAG, "Active input field detected! App: $pkgName, Class: $className")

        val current = _detectionState.value
        _detectionState.value = InputDetectionState(
            isInputActive = true,
            lastPackageName = pkgName.ifEmpty { "Active TV App" },
            lastClassName = className,
            lastDetectionTime = System.currentTimeMillis(),
            totalDetections = current.totalDetections + 1
        )

        // Check user preference
        val prefs = service.getSharedPreferences(AuraOverlayService.PREFS_NAME, Context.MODE_PRIVATE)
        val isAutoEnabled = prefs.getBoolean(AuraOverlayService.KEY_AUTO_SHOW_KEYBOARD, true)

        if (isAutoEnabled) {
            Log.d(TAG, "Auto-keyboard enabled. Triggering floating keyboard on TV!")
            AuraOverlayService.triggerFloatingKeyboard(service)
        } else {
            Log.d(TAG, "Auto-keyboard disabled in preferences; skipping popup.")
        }
    }

    private fun checkIsEditable(event: AccessibilityEvent): Boolean {
        val eventClass = event.className?.toString() ?: ""
        if (matchesEditablePattern(eventClass)) {
            return true
        }

        val source = try {
            event.source
        } catch (_: Exception) {
            null
        }

        if (source != null) {
            try {
                if (source.isEditable) return true
                val srcClass = source.className?.toString() ?: ""
                if (matchesEditablePattern(srcClass)) return true
                if (source.isFocused && source.isEditable) return true
            } catch (_: Exception) {
            }
        }

        return false
    }

    private fun matchesEditablePattern(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("edittext") ||
                lower.contains("searchview") ||
                lower.contains("autocompletetextview") ||
                lower.contains("extractededittext") ||
                lower.contains("searchautocomplete") ||
                lower.contains("textinputedittext")
    }

    private fun isSimulatedPackage(pkg: String): Boolean {
        return pkg.contains("simulated", ignoreCase = true)
    }

    /**
     * Testing hook to simulate input field focus from the TV Dashboard
     */
    fun simulateInputFocus(context: Context, appName: String = "YouTube TV Search") {
        val current = _detectionState.value
        _detectionState.value = InputDetectionState(
            isInputActive = true,
            lastPackageName = appName,
            lastClassName = "android.widget.SearchView",
            lastDetectionTime = System.currentTimeMillis(),
            totalDetections = current.totalDetections + 1
        )
        AuraOverlayService.triggerFloatingKeyboard(context)
    }

    /**
     * Clears active input field state
     */
    fun clearActiveInput() {
        val current = _detectionState.value
        _detectionState.value = current.copy(isInputActive = false)
    }
}
