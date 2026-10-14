package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.lang.ref.WeakReference

class AuraAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentInstance = WeakReference(this)

        // Ensure flags include interactive windows retrieval and node inspection
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            serviceInfo = info
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update AccessibilityServiceInfo flags", e)
        }

        Log.i(TAG, "AuraAccessibilityService connected and ready for TV Global Back and Text Input actions.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Track active editable input metadata
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            val pkg = event.packageName?.toString()
            if (pkg != null && pkg != packageName) {
                lastActivePackage = pkg
            }

            val source = try { event.source } catch (_: Exception) { null }
            if (source != null) {
                val cls = source.className?.toString() ?: ""
                val id = try { source.viewIdResourceName } catch (_: Exception) { null }
                if (source.isEditable || isInputClass(cls) || (id != null && isInputId(id))) {
                    lastActiveClassName = cls
                    if (id != null) {
                        lastActiveViewId = id
                    }
                }
            }
        }

        // Automatically detect active input fields and trigger floating keyboard
        TvInputDetector.onAccessibilityEvent(this, event)
    }

    override fun onInterrupt() {
        Log.w(TAG, "AuraAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        lastActivePackage = null
        lastActiveViewId = null
        lastActiveClassName = null
        if (currentInstance?.get() == this) {
            currentInstance = null
        }
    }

    companion object {
        private const val TAG = "AuraAccessibility"
        private var currentInstance: WeakReference<AuraAccessibilityService>? = null

        private var lastActivePackage: String? = null
        private var lastActiveViewId: String? = null
        private var lastActiveClassName: String? = null

        fun isServiceRunning(): Boolean {
            return currentInstance?.get() != null
        }

        fun isInputClass(className: String): Boolean {
            val lower = className.lowercase()
            return lower.contains("edittext") ||
                    lower.contains("searchview") ||
                    lower.contains("autocompletetextview") ||
                    lower.contains("extractededittext") ||
                    lower.contains("searchautocomplete") ||
                    lower.contains("textinputedittext") ||
                    lower.contains("searchedittext") ||
                    lower.contains("searchorb") ||
                    lower.contains("searchbar")
        }

        fun isInputId(viewId: String): Boolean {
            val lower = viewId.lowercase()
            return lower.contains("search") ||
                    lower.contains("query") ||
                    lower.contains("edit") ||
                    lower.contains("input") ||
                    lower.contains("search_src_text") ||
                    lower.contains("lb_search_bar")
        }

        private var inAppInputListener: ((String) -> Unit)? = null
        private var inAppEnterListener: (() -> Unit)? = null

        fun setInAppInputListener(onInput: ((String) -> Unit)?, onEnter: (() -> Unit)?) {
            inAppInputListener = onInput
            inAppEnterListener = onEnter
        }

        /**
         * Dispatches real GLOBAL_ACTION_BACK to whatever app is currently running on the TV.
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
         */
        fun performHomeAction(context: Context? = null): Boolean {
            val service = currentInstance?.get()
            val dispatchedByA11y = if (service != null) {
                val res = service.performGlobalAction(GLOBAL_ACTION_HOME)
                Log.d(TAG, "Dispatched GLOBAL_ACTION_HOME: $res")
                res
            } else {
                false
            }

            if (context != null) {
                try {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
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

        /**
         * Finds the currently active/focused editable input field on the TV and sets its text.
         * Employs multi-layer window inspection, class matching, and clipboard paste fallback.
         */
        fun commitTextToActiveField(text: String, context: Context? = null): Boolean {
            var anyInjected = false

            // 1. Immediately notify in-app listener (for in-app playground and test fields)
            try {
                if (inAppInputListener != null) {
                    inAppInputListener?.invoke(text)
                    anyInjected = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying inAppInputListener", e)
            }

            // 2. Always copy text to clipboard as primary safety net
            val service = currentInstance?.get()
            val ctx = context ?: service
            if (ctx != null) {
                try {
                    val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("Aura Remote Text", text))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing clipboard for commit", e)
                }
            }

            if (service == null) {
                Log.w(TAG, "AuraAccessibilityService is not connected.")
                return anyInjected
            }

            // 3. Locate target input node across active TV windows
            val targetNode = findTargetInputNode(service)
            if (targetNode == null) {
                Log.w(TAG, "No editable or focused input node found in active window or background apps.")
                return anyInjected
            }

            Log.i(TAG, "Target node found: class=${targetNode.className}, id=${targetNode.viewIdResourceName}, isEditable=${targetNode.isEditable}, isFocused=${targetNode.isFocused}")

            // 4. Ensure target has focus
            try {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                targetNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            } catch (_: Exception) {}

            // 5. Try ACTION_SET_TEXT
            var success = false
            try {
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                }
                success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                Log.i(TAG, "ACTION_SET_TEXT result: $success")
            } catch (e: Exception) {
                Log.e(TAG, "ACTION_SET_TEXT threw exception", e)
            }

            // 6. Fallback: ACTION_PASTE
            if (!success) {
                try {
                    success = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    Log.i(TAG, "ACTION_PASTE result: $success")
                } catch (e: Exception) {
                    Log.e(TAG, "ACTION_PASTE threw exception", e)
                }
            }

            // 7. Secondary fallback: click node first, then retry SET_TEXT / PASTE
            if (!success) {
                try {
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    val args = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            text
                        )
                    }
                    success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    if (!success) {
                        success = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    }
                    Log.i(TAG, "Retry with ACTION_CLICK result: $success")
                } catch (e: Exception) {
                    Log.e(TAG, "Click & retry threw exception", e)
                }
            }

            return success || anyInjected
        }

        /**
         * Dispatches Enter / Search action to the currently focused input field or search button.
         */
        fun performEnterAction(): Boolean {
            var clicked = false

            // Notify in-app listener first
            try {
                inAppEnterListener?.invoke()
            } catch (_: Exception) {}

            val service = currentInstance?.get() ?: return clicked

            // Strategy 1: Find search button or submit action in windows
            try {
                val windows = service.windows
                for (win in windows) {
                    val root = win.root ?: continue
                    // Skip the overlay window itself
                    if (isOverlayWindow(win, root, service)) continue

                    val searchBtn = findSearchButtonRecursive(root)
                    if (searchBtn != null) {
                        clicked = searchBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Clicked search button: class=${searchBtn.className}, id=${searchBtn.viewIdResourceName}, res=$clicked")
                        if (clicked) return true
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error looking for search button", e)
            }

            // Strategy 2: Click focused input node to trigger submit/search
            try {
                val target = findTargetInputNode(service)
                if (target != null) {
                    clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.i(TAG, "Clicked input node for enter: $clicked")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing enter click", e)
            }

            return clicked
        }

        private fun isOverlayWindow(win: AccessibilityWindowInfo, root: AccessibilityNodeInfo, service: AccessibilityService): Boolean {
            if (win.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) return true
            val id = root.viewIdResourceName ?: ""
            return id.contains("floating_keyboard_screen_container") || id.contains("dynamic_bubble_capsule")
        }

        private fun findTargetInputNode(service: AccessibilityService): AccessibilityNodeInfo? {
            // Strategy A: Iterate interactive windows, prioritizing application windows
            try {
                val windows = service.windows
                if (windows != null && windows.isNotEmpty()) {
                    val sortedWindows = windows.sortedByDescending { win ->
                        var score = 0
                        if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) score += 15
                        if (win.isFocused) score += 8
                        if (win.isActive) score += 4
                        val root = win.root
                        if (root != null && root.packageName != service.packageName) score += 2
                        score
                    }

                    for (win in sortedWindows) {
                        val root = win.root ?: continue
                        // Skip the floating keyboard overlay itself so we don't try to type into our own keyboard buttons
                        if (isOverlayWindow(win, root, service)) continue

                        val node = searchWindowForInput(root)
                        if (node != null) return node
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error searching windows", e)
            }

            // Strategy B: Fallback to rootInActiveWindow
            try {
                val root = service.rootInActiveWindow
                if (root != null) {
                    val node = searchWindowForInput(root)
                    if (node != null) return node
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error searching rootInActiveWindow", e)
            }

            return null
        }

        private fun searchWindowForInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            // 1. Check FOCUS_INPUT
            try {
                val focusedInput = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedInput != null) {
                    val editableChild = findEditableChildOrSelf(focusedInput)
                    if (editableChild != null) return editableChild
                }
            } catch (_: Exception) {}

            // 2. Check FOCUS_ACCESSIBILITY
            try {
                val a11yFocused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                if (a11yFocused != null) {
                    val editableChild = findEditableChildOrSelf(a11yFocused)
                    if (editableChild != null) return editableChild
                }
            } catch (_: Exception) {}

            // 3. Check by cached view ID
            val viewId = lastActiveViewId
            if (viewId != null) {
                try {
                    val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                    if (!nodes.isNullOrEmpty()) {
                        for (n in nodes) {
                            val candidate = findEditableChildOrSelf(n)
                            if (candidate != null) return candidate
                        }
                    }
                } catch (_: Exception) {}
            }

            // 4. Recursive search:
            // Pass 1: any focused editable candidate
            val focusedCandidate = findNodeRecursive(root) { it.isFocused && isLikelyInput(it) && !isKeyboardKey(it) }
            if (focusedCandidate != null) {
                return findEditableChildOrSelf(focusedCandidate) ?: focusedCandidate
            }

            // Pass 2: any isEditable field
            val editableCandidate = findNodeRecursive(root) { it.isEditable && !isKeyboardKey(it) }
            if (editableCandidate != null) return editableCandidate

            // Pass 3: any class/id matching input
            val patternCandidate = findNodeRecursive(root) { isLikelyInput(it) && !isKeyboardKey(it) }
            if (patternCandidate != null) {
                return findEditableChildOrSelf(patternCandidate) ?: patternCandidate
            }

            // Pass 4: any focused focusable node in window
            val anyFocused = findNodeRecursive(root) { it.isFocused && it.isFocusable && !isKeyboardKey(it) }
            if (anyFocused != null) {
                return findEditableChildOrSelf(anyFocused) ?: anyFocused
            }

            return null
        }

        private fun isKeyboardKey(node: AccessibilityNodeInfo): Boolean {
            val id = node.viewIdResourceName ?: ""
            return id.contains("key_") || id.contains("kb_btn") || id.contains("keyboard_screen")
        }

        private fun findEditableChildOrSelf(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable) return node
            val cls = node.className?.toString()?.lowercase() ?: ""
            if (cls.contains("edittext") || cls.contains("searchautocomplete")) return node
            for (action in node.actionList) {
                if (action.id == AccessibilityNodeInfo.ACTION_SET_TEXT || action.id == AccessibilityNodeInfo.ACTION_PASTE) {
                    return node
                }
            }

            // If it's a container (e.g. SearchBar, SearchView, FrameLayout), search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findEditableChildOrSelf(child)
                if (found != null) return found
            }
            return if (isLikelyInput(node)) node else null
        }

        private fun isLikelyInput(node: AccessibilityNodeInfo): Boolean {
            if (node.isEditable) return true
            val cls = node.className?.toString() ?: ""
            if (isInputClass(cls)) return true
            val id = node.viewIdResourceName ?: ""
            if (isInputId(id)) return true
            for (action in node.actionList) {
                if (action.id == AccessibilityNodeInfo.ACTION_SET_TEXT || action.id == AccessibilityNodeInfo.ACTION_PASTE) {
                    return true
                }
            }
            return false
        }

        private fun findNodeRecursive(
            node: AccessibilityNodeInfo,
            predicate: (AccessibilityNodeInfo) -> Boolean
        ): AccessibilityNodeInfo? {
            try {
                if (predicate(node)) return node
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    val match = findNodeRecursive(child, predicate)
                    if (match != null) return match
                }
            } catch (_: Exception) {}
            return null
        }

        private fun findSearchButtonRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            try {
                val id = node.viewIdResourceName?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val txt = node.text?.toString()?.lowercase() ?: ""

                if (node.isClickable && (
                            id.contains("search_orb") ||
                            id.contains("search_button") ||
                            id.contains("search_go") ||
                            id.contains("btn_search") ||
                            desc.contains("search") ||
                            desc.contains("submit") ||
                            txt == "search" ||
                            txt == "go"
                        )) {
                    return node
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    val match = findSearchButtonRecursive(child)
                    if (match != null) return match
                }
            } catch (_: Exception) {}
            return null
        }
    }
}

