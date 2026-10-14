package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.accessibility.AuraAccessibilityService
import com.example.keyboard.AuraTvInputMethodService
import com.example.keyboard.FloatingKeyboardState
import com.example.keyboard.KeyboardMoveDirection
import com.example.ui.theme.AuraRemoteTheme
import com.example.ui.OverlayDisplayMode
import com.example.ui.TvDynamicBubbleView
import com.example.volume.TvVolumeController
import com.example.volume.VolumeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs

class AuraOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayContainer: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private lateinit var prefs: SharedPreferences
    private lateinit var volumeController: TvVolumeController
    private val keyboardState = FloatingKeyboardState()
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private var displayMode by mutableStateOf(OverlayDisplayMode.MINIMIZED_BUBBLE)
    private var secondsRemaining by mutableIntStateOf(30)
    private var bubbleOpacity by mutableFloatStateOf(0.90f)
    private var keyboardOpacity by mutableFloatStateOf(0.90f)
    private var volumeStatus by mutableStateOf(
        VolumeStatus(0, 100, 0, false, false, "Ready")
    )

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var timerJob: Job? = null

    companion object {
        private const val TAG = "AuraOverlayService"
        private const val CHANNEL_ID = "aura_tv_remote_channel"
        private const val NOTIFICATION_ID = 4031
        const val PREFS_NAME = "aura_remote_overlay_prefs"
        const val KEY_POS_X = "overlay_x"
        const val KEY_POS_Y = "overlay_y"
        const val KEY_BUBBLE_OPACITY = "bubble_opacity"
        const val KEY_KEYBOARD_OPACITY = "keyboard_opacity"
        const val KEY_AUTO_SHOW_KEYBOARD = "auto_show_keyboard"
        const val ACTION_OPEN_KEYBOARD = "com.example.ACTION_OPEN_KEYBOARD"

        private var currentInstance: WeakReference<AuraOverlayService>? = null

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AuraOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AuraOverlayService::class.java)
            context.stopService(intent)
        }

        /**
         * Automatically triggers display of the floating keyboard on the TV.
         * If the service is running, it immediately switches display mode to FLOATING_KEYBOARD.
         * If the service is not running, it starts the foreground service and commands it to open the keyboard.
         */
        fun triggerFloatingKeyboard(context: Context) {
            val service = currentInstance?.get()
            if (service != null && isRunning) {
                service.serviceScope.launch(Dispatchers.Main) {
                    service.handleModeChange(OverlayDisplayMode.FLOATING_KEYBOARD)
                }
            } else {
                val intent = Intent(context, AuraOverlayService::class.java).apply {
                    action = ACTION_OPEN_KEYBOARD
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        /**
         * Moves the active floating keyboard in the specified direction.
         */
        fun moveCurrentKeyboard(direction: KeyboardMoveDirection) {
            val service = currentInstance?.get()
            service?.moveKeyboard(direction)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        currentInstance = WeakReference(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        bubbleOpacity = prefs.getFloat(KEY_BUBBLE_OPACITY, 0.90f)
        keyboardOpacity = prefs.getFloat(KEY_KEYBOARD_OPACITY, 0.90f)

        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == KEY_BUBBLE_OPACITY) {
                bubbleOpacity = sp.getFloat(KEY_BUBBLE_OPACITY, 0.90f)
            } else if (key == KEY_KEYBOARD_OPACITY) {
                keyboardOpacity = sp.getFloat(KEY_KEYBOARD_OPACITY, 0.90f)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        volumeController = TvVolumeController(this)
        volumeStatus = volumeController.getStatus()

        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted. Stopping service.")
            stopSelf()
            return
        }

        initOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPEN_KEYBOARD) {
            handleModeChange(OverlayDisplayMode.FLOATING_KEYBOARD)
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura TV Remote Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the dynamic floating bubble active above TV apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) {
            Log.e(TAG, "WindowManager unavailable")
            stopSelf()
            return
        }

        lifecycleOwner = OverlayLifecycleOwner().apply { start() }

        val initialX = prefs.getInt(KEY_POS_X, 80)
        val initialY = prefs.getInt(KEY_POS_Y, 80)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                AuraRemoteTheme {
                    TvDynamicBubbleView(
                        mode = displayMode,
                        secondsRemaining = secondsRemaining,
                        volumeStatus = volumeStatus,
                        keyboardState = keyboardState,
                        volumeController = volumeController,
                        bubbleOpacity = bubbleOpacity,
                        keyboardOpacity = keyboardOpacity,
                        onHomeAction = {
                            resetInactivityTimer()
                            AuraAccessibilityService.performHomeAction(this@AuraOverlayService)
                            handleModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
                        },
                        onModeChange = { newMode ->
                            handleModeChange(newMode)
                        },
                        onResetPosition = {
                            resetBubblePosition()
                        },
                        onCloseService = {
                            stopSelf()
                        },
                        onInteraction = {
                            resetInactivityTimer()
                        },
                        onMoveKeyboard = { direction ->
                            resetInactivityTimer()
                            moveKeyboard(direction)
                        },
                        onSubmitText = { submitted ->
                            resetInactivityTimer()
                            // Commit text into background active TV input field
                            AuraAccessibilityService.commitTextToActiveField(submitted, this@AuraOverlayService)
                            if (AuraTvInputMethodService.isImeActive()) {
                                AuraTvInputMethodService.commitTextFromOverlay(submitted)
                                AuraTvInputMethodService.sendEnterFromOverlay()
                            }
                            AuraAccessibilityService.performEnterAction()
                            // Auto minimize to bubble so user can see their results
                            handleModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
                        }
                    )
                }
            }
        }

        // Attach Mouse Drag vs Click TouchListener
        setupDragTouchListener(composeView)

        overlayContainer = composeView
        try {
            windowManager?.addView(overlayContainer, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view to WindowManager", e)
            stopSelf()
        }
    }

    private fun setupDragTouchListener(view: View) {
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false
        var lastUpdateTime = 0L

        view.setOnTouchListener { _, event ->
            // CRITICAL FIX: Only drag when in MINIMIZED_BUBBLE mode.
            // When in FLOATING_KEYBOARD or EXPANDED_PANEL, all touches and mouse clicks
            // are for typing keys, scrolling, and clicking buttons! Never drag or consume!
            if (displayMode != OverlayDisplayMode.MINIMIZED_BUBBLE) {
                return@setOnTouchListener false
            }

            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    false // Don't consume down yet so clicks can trigger if not dragged
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (abs(dx) > 16 || abs(dy) > 16) {
                        isDragging = true
                        val now = android.os.SystemClock.uptimeMillis()
                        if (now - lastUpdateTime > 24L) {
                            lastUpdateTime = now
                            params.x = startX + dx
                            params.y = startY + dy
                            try {
                                windowManager?.updateViewLayout(overlayContainer, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "updateViewLayout move error", e)
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        // Persist position
                        prefs.edit()
                            .putInt(KEY_POS_X, params.x)
                            .putInt(KEY_POS_Y, params.y)
                            .apply()
                        true
                    } else {
                        false // Pass click to Compose buttons
                    }
                }
                else -> false
            }
        }
    }

    private fun handleModeChange(newMode: OverlayDisplayMode) {
        displayMode = newMode
        resetInactivityTimer()

        val params = layoutParams ?: return
        when (newMode) {
            OverlayDisplayMode.MINIMIZED_BUBBLE -> {
                // Background apps should receive D-pad focus
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                stopTimer()
            }
            OverlayDisplayMode.EXPANDED_PANEL -> {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                startTimer()
            }
            OverlayDisplayMode.FLOATING_KEYBOARD -> {
                // Keep FLAG_NOT_FOCUSABLE so background apps (YouTube, Netflix, TV Search)
                // NEVER lose focus from their active input fields! All mouse/touch clicks still work 100%.
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                startTimer()
            }
        }

        try {
            windowManager?.updateViewLayout(overlayContainer, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update layout params for mode change", e)
        }
    }

    private fun startTimer() {
        stopTimer()
        secondsRemaining = 30
        timerJob = serviceScope.launch {
            while (isActive && secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
            }
            if (secondsRemaining <= 0 && displayMode != OverlayDisplayMode.MINIMIZED_BUBBLE) {
                handleModeChange(OverlayDisplayMode.MINIMIZED_BUBBLE)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun resetInactivityTimer() {
        secondsRemaining = 30
        volumeStatus = volumeController.getStatus()
        if (displayMode != OverlayDisplayMode.MINIMIZED_BUBBLE && (timerJob == null || !timerJob!!.isActive)) {
            startTimer()
        }
    }

    private fun resetBubblePosition() {
        val params = layoutParams ?: return
        params.x = 80
        params.y = 80
        prefs.edit().putInt(KEY_POS_X, 80).putInt(KEY_POS_Y, 80).apply()
        try {
            windowManager?.updateViewLayout(overlayContainer, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset position", e)
        }
    }

    /**
     * Shifts the custom floating keyboard across the screen in the specified direction.
     * Restricts movement within safe TV screen boundaries.
     */
    fun moveKeyboard(direction: KeyboardMoveDirection) {
        val params = layoutParams ?: return
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val stepX = 140
        val stepY = 100

        when (direction) {
            KeyboardMoveDirection.LEFT -> {
                params.x = (params.x - stepX).coerceAtLeast(10)
            }
            KeyboardMoveDirection.RIGHT -> {
                params.x = (params.x + stepX).coerceAtMost((screenWidth - 300).coerceAtLeast(100))
            }
            KeyboardMoveDirection.UP -> {
                params.y = (params.y - stepY).coerceAtLeast(10)
            }
            KeyboardMoveDirection.DOWN -> {
                params.y = (params.y + stepY).coerceAtMost((screenHeight - 200).coerceAtLeast(100))
            }
            KeyboardMoveDirection.CENTER -> {
                params.x = ((screenWidth - 640) / 2).coerceAtLeast(20)
                params.y = ((screenHeight - 440) / 2).coerceAtLeast(20)
            }
        }

        prefs.edit()
            .putInt(KEY_POS_X, params.x)
            .putInt(KEY_POS_Y, params.y)
            .apply()

        try {
            windowManager?.updateViewLayout(overlayContainer, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update layout params on move", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (currentInstance?.get() == this) {
            currentInstance = null
        }
        prefListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        stopTimer()
        serviceJob.cancel()
        lifecycleOwner?.destroy()
        if (overlayContainer != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayContainer)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view in onDestroy", e)
            }
            overlayContainer = null
        }
    }
}
