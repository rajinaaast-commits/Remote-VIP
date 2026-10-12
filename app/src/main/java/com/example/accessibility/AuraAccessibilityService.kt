package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class AuraAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentInstance = WeakReference(this)
        Log.i(TAG, "AuraAccessibilityService connected and ready for TV Global Back actions.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Automatically detect active input fields and trigger floating keyboard
        TvInputDetector.onAccessibilityEvent(this, event)
    }

    override fun onInterrupt() {
        Log.w(TAG, "AuraAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance?.get() == this) {
            currentInstance = null
        }
    }

    companion object {
        private const val TAG = "AuraAccessibility"
        private var currentInstance: WeakReference<AuraAccessibilityService>? = null

        fun isServiceRunning(): Boolean {
            return currentInstance?.get() != null
        }

        /**
         * Dispatches real GLOBAL_ACTION_BACK to whatever app is currently running on the TV.
         * Returns true if successfully dispatched.
         */
        fun performBackAction(): Boolean {
            val service = currentInstance?.get()
            return if (service != null) {
                val result = service.performGlobalAction(GLOBAL_ACTION_BACK)
                Log.d(TAG, "Dispatched GLOBAL_ACTION_BACK: $result")
                result
            } else {
                Log.d(TAG, "AuraAccessibilityService is not currently bound/active.")
                false
            }
        }

        /**
         * Dispatches real GLOBAL_ACTION_HOME to toggle to the TV Home Screen and close running apps.
         * Kills background app processes to free RAM and launches the TV Home launcher.
         */
        fun performHomeAction(context: android.content.Context? = null): Boolean {
            val service = currentInstance?.get()
            val dispatchedByA11y = if (service != null) {
                val res = service.performGlobalAction(GLOBAL_ACTION_HOME)
                Log.d(TAG, "Dispatched GLOBAL_ACTION_HOME: $res")
                res
            } else {
                false
            }

            if (context != null) {
                // Terminate background app processes to free the 1GB RAM on Haier TV
                try {
                    val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                    if (am != null) {
                        val runningProcesses = am.runningAppProcesses
                        runningProcesses?.forEach { proc ->
                            if (proc.processName != context.packageName &&
                                !proc.processName.contains("system") &&
                                !proc.processName.contains("com.android.systemui") &&
                                !proc.processName.contains("media")) {
                                try {
                                    am.killBackgroundProcesses(proc.processName)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning background processes on home action", e)
                }

                try {
                    val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    }
                    context.startActivity(homeIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch home intent fallback", e)
                }
            }

            return dispatchedByA11y || context != null
        }
    }
}
