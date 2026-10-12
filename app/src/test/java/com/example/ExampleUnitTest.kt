package com.example

import com.example.keyboard.FloatingKeyboardState
import com.example.ui.OverlayDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun keyboard_typingAndBackspace_worksCorrectly() {
        val state = FloatingKeyboardState()
        assertEquals("", state.text)
        assertEquals(0, state.cursorPosition)

        // Insert 'H'
        state.insertChar('H')
        assertEquals("h", state.text)
        assertEquals(1, state.cursorPosition)

        // Toggle Shift
        state.toggleShift()
        assertTrue(state.isShifted)

        // Insert 'A'
        state.insertChar('A')
        assertEquals("hA", state.text)
        assertEquals(2, state.cursorPosition)

        // Insert Space
        state.insertSpace()
        assertEquals("hA ", state.text)
        assertEquals(3, state.cursorPosition)

        // Insert "IER"
        state.insertText("IER")
        assertEquals("hA IER", state.text)
        assertEquals(6, state.cursorPosition)

        // Backspace
        state.deleteBackward()
        assertEquals("hA IE", state.text)
        assertEquals(5, state.cursorPosition)

        // Clear
        state.clear()
        assertEquals("", state.text)
        assertEquals(0, state.cursorPosition)
    }

    @Test
    fun keyboard_cursorMovement_handlesInsertions() {
        val state = FloatingKeyboardState()
        state.insertText("AC")
        state.moveCursor(1)
        state.insertText("B")
        assertEquals("ABC", state.text)
        assertEquals(2, state.cursorPosition)

        state.moveCursorLeft()
        assertEquals(1, state.cursorPosition)
        state.moveCursorRight()
        assertEquals(2, state.cursorPosition)
    }

    @Test
    fun keyboard_symbolsMode_togglesCorrectly() {
        val state = FloatingKeyboardState()
        assertFalse(state.isSymbolsMode)
        state.toggleSymbols()
        assertTrue(state.isSymbolsMode)
        state.toggleSymbols()
        assertFalse(state.isSymbolsMode)
    }

    @Test
    fun keyboard_clear_flushesAllState() {
        val state = FloatingKeyboardState()
        state.insertText("HAIER TV TEST")
        assertEquals("HAIER TV TEST", state.text)
        assertEquals(13, state.cursorPosition)
        state.clear()
        assertEquals("", state.text)
        assertEquals(0, state.cursorPosition)
    }

    @Test
    fun overlayModes_enumIntegrity() {
        val modes = OverlayDisplayMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(OverlayDisplayMode.MINIMIZED_BUBBLE))
        assertTrue(modes.contains(OverlayDisplayMode.EXPANDED_PANEL))
        assertTrue(modes.contains(OverlayDisplayMode.FLOATING_KEYBOARD))
    }
}
