package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.FloatingKeyboardState
import com.example.ui.theme.AuraCardDark
import com.example.ui.theme.AuraCyanDim
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraCyanNeon
import com.example.ui.theme.AuraDarkBg
import com.example.ui.theme.AuraGlass90
import com.example.ui.theme.AuraGreen
import com.example.ui.theme.AuraKeyBg
import com.example.ui.theme.AuraRed
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary

/**
 * Custom Floating Keyboard Screen for Android TV with 90% Background Opacity.
 *
 * Specifically engineered for Haier 43-inch TV viewing distance with:
 * - 90% opacity dark glass background (Color(0xE60D172A))
 * - 100% opaque keys, crisp high-contrast labels, and glowing neon cyan focus outlines
 * - Dedicated click handlers for character insertion (A-Z, 0-9, symbols)
 * - Dedicated backspace (character deletion) handler
 * - Dedicated clear (entire text flush) handler
 * - Precision cursor stepping (◀ Left / Right ▶)
 * - Copy/Paste clipboard bridge
 * - Full Android TV D-pad and USB wired mouse support
 */
@Composable
fun FloatingKeyboardScreen(
    keyboardState: FloatingKeyboardState,
    secondsRemaining: Int = 30,
    panelOpacity: Float = 0.90f,
    onInteraction: () -> Unit = {},
    onBackToPanel: () -> Unit = {},
    onCloseKeyboard: () -> Unit = {},
    onHomeAction: () -> Unit = {},
    onSubmitText: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Animated blinking cursor (500ms cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "CursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0D172A).copy(alpha = panelOpacity.coerceIn(0.2f, 1f)), // Configurable opacity
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    AuraCyanNeon.copy(alpha = 0.9f),
                    AuraCyanDim.copy(alpha = 0.4f),
                    AuraCyanNeon.copy(alpha = 0.8f)
                )
            )
        ),
        shadowElevation = 24.dp,
        modifier = modifier
            .width(620.dp)
            .wrapContentHeight()
            .testTag("floating_keyboard_screen_container")
            .shadow(16.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // ==========================================
            // HEADER: Back Navigation, Title & Quick Actions
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Navigation buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Back to Remote Panel Button
                    KeyboardKeyButton(
                        label = "← PANEL",
                        isSpecial = true,
                        isSmall = true,
                        onClick = {
                            onInteraction()
                            onBackToPanel()
                        },
                        testTag = "kb_btn_back_to_panel"
                    )

                    // Home Button: Closes all apps and toggles to TV home screen
                    KeyboardKeyButton(
                        label = "⌂ HOME",
                        isAccent = true,
                        isSmall = true,
                        onClick = {
                            onInteraction()
                            onHomeAction()
                        },
                        testTag = "kb_btn_home"
                    )
                }

                // Title & Mode Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AuraCyanNeon)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (keyboardState.isSymbolsMode) "AURA TV KEYBOARD • [?123]" else "AURA TV KEYBOARD • [A-Z]",
                        color = AuraCyanNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Inactivity Timer Badge & Close Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AuraCardDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${secondsRemaining}s",
                            color = if (secondsRemaining <= 5) AuraRed else AuraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    KeyboardKeyButton(
                        label = "✕",
                        isSpecial = true,
                        isSmall = true,
                        onClick = {
                            onInteraction()
                            onCloseKeyboard()
                        },
                        testTag = "kb_btn_close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================================
            // LIVE TEXT INPUT BOX (Cursor, Text State, Cursor Controls)
            // ==========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1324)) // 100% opaque inner display for maximum contrast
                    .border(1.5.dp, AuraCyanNeon, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("keyboard_text_display_field")
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current Text with Blinking Cursor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val text = keyboardState.text
                            val cursor = keyboardState.cursorPosition.coerceIn(0, text.length)
                            val textBefore = text.substring(0, cursor)
                            val textAfter = text.substring(cursor)

                            if (text.isEmpty()) {
                                Text(
                                    text = "Click keys with mouse or navigate with TV remote...",
                                    color = AuraTextMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = textBefore,
                                    color = AuraTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Blinking Cursor Indicator
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .width(2.5.dp)
                                    .height(22.dp)
                                    .background(AuraCyanNeon.copy(alpha = cursorAlpha))
                            )

                            if (text.isNotEmpty()) {
                                Text(
                                    text = textAfter,
                                    color = AuraTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Cursor Stepping Controls: ◀ Left and Right ▶
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            KeyboardKeyButton(
                                label = "◀",
                                isSmall = true,
                                isSpecial = true,
                                onClick = {
                                    onInteraction()
                                    keyboardState.moveCursorLeft()
                                },
                                testTag = "kb_cursor_left"
                            )
                            KeyboardKeyButton(
                                label = "▶",
                                isSmall = true,
                                isSpecial = true,
                                onClick = {
                                    onInteraction()
                                    keyboardState.moveCursorRight()
                                },
                                testTag = "kb_cursor_right"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text Field Sub-Bar: Length, Action Status, Copy, Paste, Clear
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pos: ${keyboardState.cursorPosition}/${keyboardState.text.length} • ${keyboardState.lastActionLog}",
                            color = AuraCyanDim,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            KeyboardKeyButton(
                                label = "COPY",
                                isSmall = true,
                                isSpecial = true,
                                onClick = {
                                    onInteraction()
                                    keyboardState.copyToClipboard(context)
                                    Toast.makeText(context, "Copied text to TV clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                testTag = "kb_btn_copy"
                            )
                            KeyboardKeyButton(
                                label = "PASTE",
                                isSmall = true,
                                isSpecial = true,
                                onClick = {
                                    onInteraction()
                                    keyboardState.pasteFromClipboard(context)
                                },
                                testTag = "kb_btn_paste"
                            )
                            KeyboardKeyButton(
                                label = "CLEAR",
                                isSmall = true,
                                isSpecial = true,
                                onClick = {
                                    onInteraction()
                                    keyboardState.clear()
                                },
                                testTag = "kb_btn_clear"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // KEYPAD SECTION: NUMBERS, LETTERS & ACTIONS
            // ==========================================

            // ROW 1: Numbers (0-9)
            val numberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (num in numberRow) {
                    KeyboardKeyButton(
                        label = num,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onInteraction()
                            keyboardState.insertText(num)
                        },
                        testTag = "key_$num"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!keyboardState.isSymbolsMode) {
                // ------------------------------------
                // ALPHABET MODE (QWERTY)
                // ------------------------------------

                // ROW 2: Q W E R T Y U I O P
                val rowQwert = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (letter in rowQwert) {
                        val displayChar = if (keyboardState.isShifted) letter.uppercase() else letter.lowercase()
                        KeyboardKeyButton(
                            label = displayChar,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onInteraction()
                                keyboardState.insertChar(letter[0])
                            },
                            testTag = "key_$letter"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ROW 3: A S D F G H J K L
                val rowAsdf = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (letter in rowAsdf) {
                        val displayChar = if (keyboardState.isShifted) letter.uppercase() else letter.lowercase()
                        KeyboardKeyButton(
                            label = displayChar,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onInteraction()
                                keyboardState.insertChar(letter[0])
                            },
                            testTag = "key_$letter"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ROW 4: Z X C V B N M
                val rowZxcv = listOf("Z", "X", "C", "V", "B", "N", "M")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (letter in rowZxcv) {
                        val displayChar = if (keyboardState.isShifted) letter.uppercase() else letter.lowercase()
                        KeyboardKeyButton(
                            label = displayChar,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onInteraction()
                                keyboardState.insertChar(letter[0])
                            },
                            testTag = "key_$letter"
                        )
                    }
                }
            } else {
                // ------------------------------------
                // SYMBOLS MODE (!#$)
                // ------------------------------------
                val symRow1 = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (sym in symRow1) {
                        KeyboardKeyButton(
                            label = sym,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onInteraction()
                                keyboardState.insertText(sym)
                            },
                            testTag = "key_sym_$sym"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val symRow2 = listOf("-", "_", "+", "=", "[", "]", "{", "}", "\\", "|")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (sym in symRow2) {
                        KeyboardKeyButton(
                            label = sym,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onInteraction()
                                keyboardState.insertText(sym)
                            },
                            testTag = "key_sym_$sym"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val symRow3 = listOf(":", ";", "\"", "'", "<", ">", ",", ".", "/", "?")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (sym in symRow3) {
                        KeyboardKeyButton(
                            label = sym,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onInteraction()
                                keyboardState.insertText(sym)
                            },
                            testTag = "key_sym_$sym"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==============================================================
            // ROW 5: ACTION CONTROLS (SHIFT, SYMBOLS, SPACE, BACKSPACE, ENTER)
            // ==============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Shift Key (Caps Toggle)
                KeyboardKeyButton(
                    label = if (keyboardState.isShifted) "SHIFT ▲" else "shift ▼",
                    isSpecial = true,
                    isAccent = keyboardState.isShifted,
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        onInteraction()
                        keyboardState.toggleShift()
                    },
                    testTag = "key_shift"
                )

                // Symbols Mode Toggle Key (?123 / ABC)
                KeyboardKeyButton(
                    label = if (keyboardState.isSymbolsMode) "ABC" else "?123",
                    isSpecial = true,
                    modifier = Modifier.weight(1.2f),
                    onClick = {
                        onInteraction()
                        keyboardState.toggleSymbols()
                    },
                    testTag = "key_symbols_toggle"
                )

                // Wide Space Key
                KeyboardKeyButton(
                    label = "SPACE ␣",
                    modifier = Modifier.weight(3.2f),
                    onClick = {
                        onInteraction()
                        keyboardState.insertSpace()
                    },
                    testTag = "key_space"
                )

                // Backspace (Delete character) Key
                KeyboardKeyButton(
                    label = "⌫ DEL",
                    isSpecial = true,
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        onInteraction()
                        keyboardState.deleteBackward()
                    },
                    testTag = "key_backspace"
                )

                // Enter / Submit Key
                KeyboardKeyButton(
                    label = "ENTER ⏎",
                    isAccent = true,
                    modifier = Modifier.weight(1.8f),
                    onClick = {
                        onInteraction()
                        keyboardState.submit { submitted ->
                            onSubmitText(submitted)
                            Toast.makeText(context, "Confirmed text: \"$submitted\"", Toast.LENGTH_SHORT).show()
                        }
                    },
                    testTag = "key_enter"
                )
            }
        }
    }
}

/**
 * Reusable, High-Contrast TV Keyboard Key with:
 * - 100% opaque surface for maximum contrast over 90% opacity panel
 * - High-visibility glowing cyan outline when focused with TV D-pad or hovered with USB mouse
 * - Audible/haptic-ready click handler for real character updates
 */
@Composable
fun KeyboardKeyButton(
    label: String,
    isAccent: Boolean = false,
    isSpecial: Boolean = false,
    isSmall: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val normalBg = when {
        isAccent -> AuraCyanNeon
        isSpecial -> Color(0xFF24344D)
        else -> AuraKeyBg
    }

    val activeBg = when {
        isAccent -> Color.White
        else -> AuraCyanNeon
    }

    val normalTextColor = when {
        isAccent -> AuraDarkBg
        else -> AuraTextPrimary
    }

    val activeTextColor = AuraDarkBg

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = if (isFocused || isHovered) activeBg else normalBg,
        border = BorderStroke(
            width = if (isFocused || isHovered) 2.5.dp else 1.dp,
            color = if (isFocused || isHovered) AuraCyanNeon else Color(0xFF2C3E5B)
        ),
        shadowElevation = if (isFocused || isHovered) 10.dp else 2.dp,
        modifier = modifier
            .testTag(testTag)
            .focusable(interactionSource = interactionSource)
            .height(if (isSmall) 34.dp else 48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = label,
                color = if (isFocused || isHovered) activeTextColor else normalTextColor,
                fontSize = if (isSmall) 11.sp else if (label.length > 2) 12.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
