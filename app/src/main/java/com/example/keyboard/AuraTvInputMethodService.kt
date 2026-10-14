package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import java.lang.ref.WeakReference

class AuraTvInputMethodService : InputMethodService() {

    private var isShift = false

    companion object {
        private const val TAG = "AuraTvInputMethod"
        private var currentInstance: WeakReference<AuraTvInputMethodService>? = null

        fun isImeActive(): Boolean {
            return currentInstance?.get() != null
        }

        fun commitTextFromOverlay(text: String): Boolean {
            val ime = currentInstance?.get() ?: return false
            val ic = ime.currentInputConnection ?: return false
            val ok = ic.commitText(text, 1)
            Log.d(TAG, "commitTextFromOverlay: $ok for text: \"$text\"")
            return ok
        }

        fun sendEnterFromOverlay(): Boolean {
            val ime = currentInstance?.get() ?: return false
            val ic = ime.currentInputConnection
            if (ic != null) {
                try {
                    ic.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
                    ic.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_GO)
                    ic.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing editor actions on IME InputConnection", e)
                }
            }
            try {
                ime.sendKeyChar('\n')
                ime.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            } catch (_: Exception) {}
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentInstance = WeakReference(this)
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentInstance = WeakReference(this)
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInstance = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance?.get() == this) {
            currentInstance = null
        }
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E60D172A")) // 90% opacity dark slate
            setPadding(24, 16, 24, 20)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 0, 8, 8)
        }
        val titleText = TextView(this).apply {
            text = "AURA REMOTE TV KEYBOARD • USB MOUSE & D-PAD READY"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        titleRow.addView(titleText)
        root.addView(titleRow)

        // Keyboard rows: Numbers 0-9, QWERTY rows, Space & Actions
        val rows = listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M")
        )

        for (rowKeys in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            for (key in rowKeys) {
                rowLayout.addView(createKeyButton(key) {
                    val charToCommit = if (isShift) key.uppercase() else key.lowercase()
                    currentInputConnection?.commitText(charToCommit, 1)
                })
            }
            root.addView(rowLayout)
        }

        // Action row: SHIFT, SPACE, BACKSPACE, ENTER, CLOSE
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 4)
        }

        val shiftBtn = createKeyButton("SHIFT") {
            isShift = !isShift
        }
        actionRow.addView(shiftBtn)

        val spaceBtn = createKeyButton("SPACE", flexWeight = 3f) {
            currentInputConnection?.commitText(" ", 1)
        }
        actionRow.addView(spaceBtn)

        val backspaceBtn = createKeyButton("⌫ DEL") {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        actionRow.addView(backspaceBtn)

        val enterBtn = createKeyButton("ENTER ⏎", isPrimary = true) {
            sendKeyChar('\n')
        }
        actionRow.addView(enterBtn)

        val hideBtn = createKeyButton("HIDE ✕") {
            requestHideSelf(0)
        }
        actionRow.addView(hideBtn)

        root.addView(actionRow)

        return root
    }

    private fun createKeyButton(
        label: String,
        isPrimary: Boolean = false,
        flexWeight: Float = 1f,
        onClick: () -> Unit
    ): View {
        val btn = Button(this).apply {
            text = label
            textSize = 14f
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            setTextColor(if (isPrimary) Color.parseColor("#0A0F1D") else Color.parseColor("#F8FAFC"))
            
            val normalBg = GradientDrawable().apply {
                setColor(if (isPrimary) Color.parseColor("#00E5FF") else Color.parseColor("#1E2D44"))
                cornerRadius = 12f
                setStroke(2, Color.parseColor(if (isPrimary) "#00E5FF" else "#2A3D5B"))
            }
            val focusedBg = GradientDrawable().apply {
                setColor(Color.parseColor("#00B4D8"))
                cornerRadius = 12f
                setStroke(4, Color.parseColor("#00E5FF"))
            }

            background = normalBg
            setOnFocusChangeListener { _, hasFocus ->
                background = if (hasFocus) focusedBg else normalBg
            }

            setOnClickListener {
                onClick()
            }

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                flexWeight
            ).apply {
                setMargins(4, 2, 4, 2)
            }
        }
        return btn
    }
}
