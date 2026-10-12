package com.example.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FloatingKeyboardState {
    var text by mutableStateOf("")
        private set

    var cursorPosition by mutableIntStateOf(0)
        private set

    var isShifted by mutableStateOf(false)
        private set

    var isSymbolsMode by mutableStateOf(false)
        private set

    var lastActionLog by mutableStateOf("Keyboard ready. Click keys with mouse or use D-pad + OK.")
        private set

    fun insertText(string: String) {
        val safeCursor = cursorPosition.coerceIn(0, text.length)
        val before = text.substring(0, safeCursor)
        val after = text.substring(safeCursor)
        text = before + string + after
        cursorPosition = safeCursor + string.length
        lastActionLog = "Inserted: \"$string\""
    }

    fun insertChar(char: Char) {
        val actualChar = if (isShifted) char.uppercaseChar() else char.lowercaseChar()
        insertText(actualChar.toString())
    }

    fun deleteBackward() {
        if (text.isEmpty() || cursorPosition <= 0) {
            lastActionLog = "Backspace: text already empty"
            return
        }
        val safeCursor = cursorPosition.coerceIn(1, text.length)
        val before = text.substring(0, safeCursor - 1)
        val after = text.substring(safeCursor)
        text = before + after
        cursorPosition = safeCursor - 1
        lastActionLog = "Backspace deleted character"
    }

    fun insertSpace() {
        insertText(" ")
        lastActionLog = "Space inserted"
    }

    fun clear() {
        text = ""
        cursorPosition = 0
        lastActionLog = "Input text cleared"
    }

    fun toggleShift() {
        isShifted = !isShifted
        lastActionLog = if (isShifted) "Shift ON (Uppercase)" else "Shift OFF (Lowercase)"
    }

    fun toggleSymbols() {
        isSymbolsMode = !isSymbolsMode
        lastActionLog = if (isSymbolsMode) "Symbols Mode (!#$)" else "Alphabet Mode (ABC)"
    }

    fun moveCursorLeft() {
        if (cursorPosition > 0) {
            cursorPosition--
            lastActionLog = "Cursor: $cursorPosition / ${text.length}"
        }
    }

    fun moveCursorRight() {
        if (cursorPosition < text.length) {
            cursorPosition++
            lastActionLog = "Cursor: $cursorPosition / ${text.length}"
        }
    }

    fun pasteFromClipboard(context: Context): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pasteText = clip.getItemAt(0).coerceToText(context)?.toString() ?: ""
            if (pasteText.isNotEmpty()) {
                insertText(pasteText)
                lastActionLog = "Pasted ${pasteText.length} chars from clipboard"
                return true
            }
        }
        lastActionLog = "Clipboard is empty"
        return false
    }

    fun moveCursor(newPos: Int) {
        cursorPosition = newPos.coerceIn(0, text.length)
    }

    fun copyToClipboard(context: Context): Boolean {
        if (text.isEmpty()) {
            lastActionLog = "Clipboard: Nothing to copy"
            return false
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Aura Remote Text", text)
            clipboard.setPrimaryClip(clip)
            lastActionLog = "Copied \"$text\" to TV Clipboard!"
            return true
        }
        lastActionLog = "Clipboard service unavailable"
        return false
    }

    fun submit(onSubmit: ((String) -> Unit)? = null) {
        lastActionLog = "Submitted: \"$text\""
        onSubmit?.invoke(text)
    }
}
