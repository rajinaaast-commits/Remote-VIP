package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.AuraAccessibilityService
import com.example.keyboard.FloatingKeyboardState
import com.example.ui.theme.AuraCardDark
import com.example.ui.theme.AuraCyanDim
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraCyanNeon
import com.example.ui.theme.AuraDarkBg
import com.example.ui.theme.AuraGlass90
import com.example.ui.theme.AuraGreen
import com.example.ui.theme.AuraKeyBg
import com.example.ui.theme.AuraKeyHover
import com.example.ui.theme.AuraRed
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.volume.TvVolumeController
import com.example.volume.VolumeStatus

enum class OverlayDisplayMode {
    MINIMIZED_BUBBLE,
    EXPANDED_PANEL,
    FLOATING_KEYBOARD
}

/**
 * iOS Style Dynamic Bubble & Floating Remote Panel for Android TV.
 * Optimized for Haier 43-inch Android TV with USB Mouse clicks and TV D-pad / OK keys.
 */
@Composable
fun TvDynamicBubbleView(
    mode: OverlayDisplayMode,
    secondsRemaining: Int,
    volumeStatus: VolumeStatus,
    keyboardState: FloatingKeyboardState,
    volumeController: TvVolumeController,
    bubbleOpacity: Float = 0.90f,
    keyboardOpacity: Float = 0.90f,
    gestureFeedback: String? = null,
    onHomeAction: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrev: () -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onModeChange: (OverlayDisplayMode) -> Unit,
    onResetPosition: () -> Unit,
    onCloseService: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .wrapContentSize()
            .animateContentSize(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
    ) {
        when (mode) {
            OverlayDisplayMode.MINIMIZED_BUBBLE -> {
                DynamicCapsuleBubble(
                    bubbleOpacity = bubbleOpacity,
                    gestureFeedback = gestureFeedback,
                    onExpand = {
                        onInteraction()
                        onModeChange(OverlayDisplayMode.EXPANDED_PANEL)
                    },
                    onHomeAction = {
                        onInteraction()
                        val success = AuraAccessibilityService.performHomeAction(context)
                        Toast.makeText(context, "Closed apps & toggled to TV Home", Toast.LENGTH_SHORT).show()
                        onHomeAction()
                    },
                    onSwipeUp = {
                        onInteraction()
                        onSwipeUp()
                    },
                    onSwipeDown = {
                        onInteraction()
                        onSwipeDown()
                    },
                    onSwipeLeft = {
                        onInteraction()
                        onSwipeLeft()
                    },
                    onSwipeRight = {
                        onInteraction()
                        onSwipeRight()
                    }
                )
            }
            OverlayDisplayMode.EXPANDED_PANEL -> {
                ExpandedRemotePanel(
                    secondsRemaining = secondsRemaining,
                    volumeStatus = volumeStatus,
                    bubbleOpacity = bubbleOpacity,
                    onVolumeUp = {
                        onInteraction()
                        volumeController.volumeUp()
                    },
                    onVolumeDown = {
                        onInteraction()
                        volumeController.volumeDown()
                    },
                    onToggleMute = {
                        onInteraction()
                        volumeController.toggleMute()
                    },
                    onOpenKeyboard = {
                        onInteraction()
                        onModeChange(OverlayDisplayMode.FLOATING_KEYBOARD)
                    },
                    onMinimize = {
                        onInteraction()
                        onModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
                    },
                    onHomeAction = {
                        onInteraction()
                        val success = AuraAccessibilityService.performHomeAction(context)
                        onModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
                        Toast.makeText(context, "Closed apps & toggled to TV Home", Toast.LENGTH_SHORT).show()
                        onHomeAction()
                    },
                    onTvBackAction = {
                        onInteraction()
                        val success = AuraAccessibilityService.performBackAction()
                        if (!success) {
                            Toast.makeText(
                                context,
                                "Enable Aura Remote in TV Settings -> Accessibility to send Back key to background apps.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "Dispatched TV Back action", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMediaNext = {
                        onInteraction()
                        onMediaNext()
                    },
                    onMediaPrev = {
                        onInteraction()
                        onMediaPrev()
                    },
                    onMediaPlayPause = {
                        onInteraction()
                        onMediaPlayPause()
                    },
                    onResetPosition = {
                        onInteraction()
                        onResetPosition()
                    },
                    onCloseService = {
                        onInteraction()
                        onCloseService()
                    }
                )
            }
            OverlayDisplayMode.FLOATING_KEYBOARD -> {
                FloatingKeyboardView(
                    keyboardState = keyboardState,
                    secondsRemaining = secondsRemaining,
                    keyboardOpacity = keyboardOpacity,
                    onInteraction = onInteraction,
                    onBackToPanel = {
                        onInteraction()
                        onModeChange(OverlayDisplayMode.EXPANDED_PANEL)
                    },
                    onCloseKeyboard = {
                        onInteraction()
                        onModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
                    },
                    onHomeAction = {
                        onInteraction()
                        val success = AuraAccessibilityService.performHomeAction(context)
                        onModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
                        Toast.makeText(context, "Closed apps & toggled to TV Home", Toast.LENGTH_SHORT).show()
                        onHomeAction()
                    }
                )
            }
        }
    }
}

/**
 * Minimized iOS Dynamic Bubble (Pill / Capsule) with Gesture Support & Direct 1-Click TV Home Button
 */
@Composable
fun DynamicCapsuleBubble(
    bubbleOpacity: Float = 0.90f,
    gestureFeedback: String? = null,
    onExpand: () -> Unit,
    onHomeAction: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "AuraPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0A0F1D).copy(alpha = bubbleOpacity.coerceIn(0.2f, 1f)), // Configurable opacity
        border = BorderStroke(
            width = if (isFocused || isHovered || gestureFeedback != null) 2.5.dp else 1.5.dp,
            brush = if (isFocused || isHovered || gestureFeedback != null) {
                Brush.horizontalGradient(listOf(AuraCyanNeon, Color.White, AuraCyanNeon))
            } else {
                Brush.horizontalGradient(listOf(AuraCyanDim, AuraCyanGlow))
            }
        ),
        shadowElevation = if (isFocused || isHovered || gestureFeedback != null) 16.dp else 8.dp,
        modifier = Modifier
            .testTag("dynamic_bubble_capsule")
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumX += dragAmount.x
                        dragAccumY += dragAmount.y
                    },
                    onDragEnd = {
                        val absX = kotlin.math.abs(dragAccumX)
                        val absY = kotlin.math.abs(dragAccumY)
                        if (absY > 25f && absY > absX * 1.15f) {
                            if (dragAccumY < 0) onSwipeUp() else onSwipeDown()
                        } else if (absX > 25f && absX > absY * 1.15f) {
                            if (dragAccumX > 0) onSwipeRight() else onSwipeLeft()
                        }
                    }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // Main Capsule pill area (click to expand full control panel)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onExpand
                    )
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                if (gestureFeedback != null) {
                    // Animated Gesture HUD Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(AuraCyanGlow)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AuraCyanNeon)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = gestureFeedback,
                            color = AuraCyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    // Default Capsule pill branding
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AuraCyanNeon.copy(alpha = pulseAlpha))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Aura TV Remote",
                        tint = AuraCyanNeon,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "AURA",
                        color = AuraTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard available",
                        tint = AuraCyanDim,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Subtle vertical separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(AuraCyanDim.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Dedicated One-Click Home Button directly on the bubble:
            // Totally closes background apps & toggles directly to the Android TV Home launcher screen
            val homeInteraction = remember { MutableInteractionSource() }
            val isHomeFocused by homeInteraction.collectIsFocusedAsState()
            val isHomeHovered by homeInteraction.collectIsHoveredAsState()

            Surface(
                onClick = onHomeAction,
                interactionSource = homeInteraction,
                shape = CircleShape,
                color = if (isHomeFocused || isHomeHovered) AuraCyanNeon else Color(0xFF16233B),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isHomeFocused || isHomeHovered) Color.White else AuraCyanDim.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .size(34.dp)
                    .testTag("btn_bubble_direct_home")
                    .focusable(interactionSource = homeInteraction)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Close all apps and toggle to TV Home",
                        tint = if (isHomeFocused || isHomeHovered) AuraDarkBg else AuraCyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Expanded Control Panel for 43-inch TV
 */
@Composable
fun ExpandedRemotePanel(
    secondsRemaining: Int,
    volumeStatus: VolumeStatus,
    bubbleOpacity: Float = 0.90f,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onMinimize: () -> Unit,
    onHomeAction: () -> Unit,
    onTvBackAction: () -> Unit,
    onMediaNext: () -> Unit = {},
    onMediaPrev: () -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onResetPosition: () -> Unit,
    onCloseService: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0D172A).copy(alpha = bubbleOpacity.coerceIn(0.2f, 1f)), // Configurable opacity
        border = BorderStroke(1.5.dp, AuraCyanDim.copy(alpha = 0.7f)),
        shadowElevation = 20.dp,
        modifier = Modifier
            .width(360.dp)
            .wrapContentHeight()
            .testTag("expanded_remote_panel")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header: Dynamic pill title + Auto-minimize timer + Quick window actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AuraCyanNeon)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aura REMOTE",
                        color = AuraCyanNeon,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Inactivity timer badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraCardDark)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Auto-hide: ${secondsRemaining}s",
                        color = if (secondsRemaining <= 5) AuraRed else AuraTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Window action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetPosition,
                        modifier = Modifier.size(32.dp).testTag("btn_reset_pos")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Bubble Position",
                            tint = AuraTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onCloseService,
                        modifier = Modifier.size(32.dp).testTag("btn_close_service")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Floating Assistant",
                            tint = AuraRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume Feedback Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraCardDark)
                    .border(1.dp, AuraCyanDim.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (volumeStatus.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume status",
                            tint = if (volumeStatus.isMuted) AuraRed else AuraCyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TV Volume: ${volumeStatus.percentage}%",
                                color = AuraTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = volumeStatus.lastActionMessage,
                                color = AuraTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Mini Mute Toggle
                    TvRemoteButton(
                        text = if (volumeStatus.isMuted) "UNMUTE" else "MUTE",
                        isSmall = true,
                        onClick = onToggleMute,
                        testTag = "btn_mute"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Required Primary Controls:
            // 1. Volume +
            // 2. Volume -
            // 3. Keyboard
            // 4. Minimize
            // 5. Back button for tv running app

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvRemoteButton(
                    text = "VOL +",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    modifier = Modifier.weight(1f),
                    onClick = onVolumeUp,
                    testTag = "btn_vol_up"
                )
                TvRemoteButton(
                    text = "VOL −",
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    modifier = Modifier.weight(1f),
                    onClick = onVolumeDown,
                    testTag = "btn_vol_down"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvRemoteButton(
                    text = "KEYBOARD",
                    icon = Icons.Default.Keyboard,
                    isAccent = true,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenKeyboard,
                    testTag = "btn_open_keyboard"
                )
                TvRemoteButton(
                    text = "MINIMIZE",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    modifier = Modifier.weight(1f),
                    onClick = onMinimize,
                    testTag = "btn_minimize"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: TV Home Button & Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TV Home Button: Closes all apps & toggles directly to TV launcher home screen
                TvRemoteButton(
                    text = "HOME ⌂",
                    icon = Icons.Default.Home,
                    isAccent = true,
                    modifier = Modifier.weight(1f),
                    onClick = onHomeAction,
                    testTag = "btn_tv_home"
                )

                // Back button for TV running app
                TvRemoteButton(
                    text = "TV BACK ⮌",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    isSecondary = true,
                    modifier = Modifier.weight(1f),
                    onClick = onTvBackAction,
                    testTag = "btn_tv_app_back"
                )
            }
        }
    }
}

/**
 * Custom Floating Keyboard with configurable panel opacity
 */
@Composable
fun FloatingKeyboardView(
    keyboardState: FloatingKeyboardState,
    secondsRemaining: Int,
    keyboardOpacity: Float = 0.90f,
    onInteraction: () -> Unit,
    onBackToPanel: () -> Unit,
    onCloseKeyboard: () -> Unit,
    onHomeAction: () -> Unit = {}
) {
    FloatingKeyboardScreen(
        keyboardState = keyboardState,
        secondsRemaining = secondsRemaining,
        panelOpacity = keyboardOpacity,
        onInteraction = onInteraction,
        onBackToPanel = onBackToPanel,
        onCloseKeyboard = onCloseKeyboard,
        onHomeAction = onHomeAction
    )
}

/**
 * TV Remote Button component with D-pad focus highlight and mouse hover
 */
@Composable
fun TvRemoteButton(
    text: String,
    icon: ImageVector? = null,
    isAccent: Boolean = false,
    isSecondary: Boolean = false,
    isSmall: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        isFocused || isHovered -> if (isAccent) AuraCyanNeon else AuraCyanDim
        isAccent -> AuraCyanNeon
        isSecondary -> AuraCardDark
        else -> AuraKeyBg
    }

    val contentColor = when {
        isFocused || isHovered -> AuraDarkBg
        isAccent -> AuraDarkBg
        else -> AuraTextPrimary
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isFocused || isHovered) 3.dp else 1.dp,
            color = if (isFocused || isHovered) AuraCyanNeon else AuraCyanDim.copy(alpha = 0.4f)
        ),
        shadowElevation = if (isFocused || isHovered) 12.dp else 4.dp,
        modifier = modifier
            .testTag(testTag)
            .focusable(interactionSource = interactionSource)
            .height(if (isSmall) 36.dp else 52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(if (isSmall) 16.dp else 22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = if (isSmall) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Individual Keyboard Key for 43" TV screen with D-pad focus and USB mouse support
 */
@Composable
fun TvKey(
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
        isSpecial -> Color(0xFF24334C)
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
        shadowElevation = if (isFocused || isHovered) 8.dp else 2.dp,
        modifier = modifier
            .testTag(testTag)
            .focusable(interactionSource = interactionSource)
            .height(if (isSmall) 32.dp else 46.dp)
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
