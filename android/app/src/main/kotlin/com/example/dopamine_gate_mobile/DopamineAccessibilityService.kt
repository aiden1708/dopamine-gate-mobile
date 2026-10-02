package com.example.dopamine_gate_mobile

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class DopamineAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DopamineGate"

        /*
         * Prevent rapid duplicate processing while a blocked app
         * is loading and generating multiple accessibility events.
         */
        private const val DEBOUNCE_MS = 800L

        /*
         * After deliberately leaving a blocked app, Android may still
         * send stale window events from that application.
         *
         * Ignore those events for this amount of time.
         */
        private const val LEAVE_COOLDOWN_MS = 2000L
    }

    private var windowManager: WindowManager? = null

    private var overlayView: View? = null

    private var isOverlayShowing = false

    /*
     * True while the user is deliberately leaving a blocked app.
     *
     * IMPORTANT:
     *
     * This is NOT cleared simply because Dopamine Gate becomes
     * foreground. Accessibility events are asynchronous and Android
     * can still deliver stale events from the previous application.
     */
    private var isLeavingBlockedApp = false

    /*
     * Absolute timestamp until which blocked-app events should be ignored.
     *
     * This is the main protection against the stale-event loop.
     */
    private var leaveCooldownUntil = 0L

    /*
     * Used to prevent duplicate blocked-app events from repeatedly
     * creating the overlay.
     */
    private var lastTriggerTime = 0L

    private val handler = Handler(Looper.getMainLooper())

    /*
     * Audio focus used to pause background media while the Gate
     * is being displayed.
     */
    private var audioManager: AudioManager? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        audioManager =
            getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        Log.d(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        /*
         * We only care about changes to the currently visible window.
         */
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        /*
         * ------------------------------------------------------------
         * 1. Ignore Dopamine Gate's own window events.
         * ------------------------------------------------------------
         *
         * We deliberately DO NOT clear isLeavingBlockedApp here.
         *
         * This was the source of the previous race condition.
         */
        if (packageName == applicationContext.packageName) {
            Log.d(TAG, "Dopamine Gate is in foreground")
            return
        }

        /*
         * ------------------------------------------------------------
         * 2. Transition protection.
         * ------------------------------------------------------------
         *
         * Once the user chooses to leave a blocked app, Android may
         * continue sending events from that application for a short
         * period of time.
         *
         * Ignore ALL window events during this transition.
         */
        if (isLeavingBlockedApp) {
            Log.d(
                TAG,
                "Leaving blocked app — ignoring event from $packageName"
            )
            return
        }

        /*
         * Additional timestamp-based protection.
         *
         * This remains effective even if isLeavingBlockedApp has
         * somehow been cleared elsewhere.
         */
        if (currentTime < leaveCooldownUntil) {
            Log.d(
                TAG,
                "Leave cooldown active — ignoring event from $packageName"
            )
            return
        }

        /*
         * ------------------------------------------------------------
         * 3. Keep an existing overlay alive.
         * ------------------------------------------------------------
         *
         * A blocked application may generate several window events
         * while loading. We don't want multiple overlays.
         */
        if (isOverlayShowing) {
            return
        }

        /*
         * ------------------------------------------------------------
         * 4. Only protected applications are handled.
         * ------------------------------------------------------------
         */
        if (!ProtectedApps.packages.contains(packageName)) {
            return
        }

        /*
         * ------------------------------------------------------------
         * 5. Check the allowed schedule.
         * ------------------------------------------------------------
         *
         * During an allowed window, the application is usable normally.
         */
        if (isWithinAllowedWindow()) {
            Log.d(
                TAG,
                "Allowed time — $packageName will not be blocked"
            )
            return
        }

        /*
         * ------------------------------------------------------------
         * 6. Debounce duplicate accessibility events.
         * ------------------------------------------------------------
         */
        if (currentTime - lastTriggerTime < DEBOUNCE_MS) {
            Log.d(
                TAG,
                "Ignoring duplicate blocked-app event: $packageName"
            )
            return
        }

        lastTriggerTime = currentTime

        Log.d(
            TAG,
            "BLOCKED APP DETECTED: $packageName"
        )

        /*
         * Pause background media immediately.
         */
        muteBackgroundAudio()

        /*
         * Display the Gate.
         */
        showOverlay(packageName)
    }

    private fun muteBackgroundAudio() {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val playbackAttributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()

                audioFocusRequest =
                    AudioFocusRequest.Builder(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    )
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener { }
                        .build()

                audioManager?.requestAudioFocus(
                    audioFocusRequest!!
                )

            } else {

                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            Log.d(
                TAG,
                "Audio focus requested — background media paused"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to request audio focus",
                e
            )
        }
    }

    private fun releaseAudioMute() {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                audioFocusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it)
                }

            } else {

                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }

            audioFocusRequest = null

            Log.d(
                TAG,
                "Audio focus released"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to release audio focus",
                e
            )
        }
    }

    private fun isWithinAllowedWindow(): Boolean {

        val calendar = Calendar.getInstance()

        val currentMinutes =
            calendar.get(Calendar.HOUR_OF_DAY) * 60 +
                calendar.get(Calendar.MINUTE)

        return GateSchedule.allowedWindows.any { window ->
            window.contains(currentMinutes)
        }
    }

    private fun showOverlay(blockedPackage: String) {

        /*
         * Absolute single-instance protection.
         */
        if (isOverlayShowing || overlayView != null) {

            Log.d(
                TAG,
                "Overlay already exists — not creating another"
            )

            return
        }

        val appName =
            ProtectedApps.getAppName(blockedPackage)

        val overlayType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            } else {

                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val layoutParams =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,

                /*
                 * The overlay must receive touch input so that the
                 * blocked application underneath cannot be interacted with.
                 */
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,

                PixelFormat.TRANSLUCENT
            ).apply {

                gravity = Gravity.CENTER
            }

        val container =
            LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setBackgroundColor(
                    Color.parseColor("#121212")
                )

                setPadding(
                    60,
                    60,
                    60,
                    60
                )

                isClickable = true

                isFocusable = true
            }

        val title =
            TextView(this).apply {

                text = "$appName is Locked!"

                textSize = 26f

                setTextColor(Color.WHITE)

                gravity = Gravity.CENTER
            }

        val subtitle =
            TextView(this).apply {

                text =
                    "Take a breath.\n\n" +
                    "Do you really need to open " +
                    "$appName right now?"

                textSize = 16f

                setTextColor(Color.LTGRAY)

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    30,
                    0,
                    50
                )
            }

        val exitButton =
            Button(this).apply {

                text = "Go to Dopamine Gate"

                setBackgroundColor(
                    Color.parseColor("#6200EE")
                )

                setTextColor(Color.WHITE)

                setPadding(
                    40,
                    20,
                    40,
                    20
                )

                setOnClickListener {

                    /*
                     * Prevent multiple taps from starting
                     * multiple navigation sequences.
                     */
                    if (isLeavingBlockedApp) {
                        return@setOnClickListener
                    }

                    /*
                     * ------------------------------------------------
                     * START TRANSITION LOCK
                     * ------------------------------------------------
                     *
                     * This is the critical part of the fix.
                     *
                     * From this moment onward, stale accessibility
                     * events from the blocked application are ignored.
                     */
                    isLeavingBlockedApp = true

                    leaveCooldownUntil =
                        System.currentTimeMillis() +
                            LEAVE_COOLDOWN_MS

                    Log.d(
                        TAG,
                        "User chose Go to Dopamine Gate — " +
                            "leaving $appName"
                    )

                    Log.d(
                        TAG,
                        "Leave cooldown started for " +
                            "${LEAVE_COOLDOWN_MS}ms"
                    )

                    /*
                     * 1. Navigate backward from the blocked app.
                     */
                    performGlobalAction(
                        GLOBAL_ACTION_BACK
                    )

                    Log.d(
                        TAG,
                        "GLOBAL_ACTION_BACK executed"
                    )

                    /*
                     * 2. Launch Dopamine Gate.
                     *
                     * We don't need to wait for an accessibility
                     * event saying that Dopamine Gate is foreground.
                     *
                     * The transition lock already protects us.
                     */
                    handler.postDelayed(
                        {

                            try {

                                val intent =
                                    Intent(
                                        applicationContext,
                                        MainActivity::class.java
                                    ).apply {

                                        putExtra(
                                            "fromBlockedApp",
                                            true
                                        )

                                        addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                        )
                                    }

                                applicationContext.startActivity(
                                    intent
                                )

                                Log.d(
                                    TAG,
                                    "Dopamine Gate MainActivity launched"
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Failed to launch Dopamine Gate",
                                    e
                                )
                            }

                        },
                        150L
                    )

                    /*
                     * 3. Remove the overlay after MainActivity has
                     * had a short opportunity to appear.
                     *
                     * The transition lock remains active even after
                     * the overlay disappears.
                     */
                    handler.postDelayed(
                        {

                            removeOverlay()

                            releaseAudioMute()

                            Log.d(
                                TAG,
                                "Overlay removed — " +
                                    "leave cooldown remains active"
                            )

                        },
                        250L
                    )

                    /*
                     * 4. End the transition lock only after the
                     * complete cooldown period has passed.
                     *
                     * This is intentionally NOT tied to an
                     * AccessibilityEvent.
                     */
                    handler.postDelayed(
                        {

                            isLeavingBlockedApp = false

                            Log.d(
                                TAG,
                                "Leave cooldown completed — " +
                                    "normal protection resumed"
                            )

                        },
                        LEAVE_COOLDOWN_MS
                    )
                }
            }

        container.addView(title)

        container.addView(subtitle)

        container.addView(exitButton)

        try {

            windowManager?.addView(
                container,
                layoutParams
            )

            overlayView = container

            isOverlayShowing = true

            Log.d(
                TAG,
                "Overlay screen drawn successfully"
            )

        } catch (e: Exception) {

            overlayView = null

            isOverlayShowing = false

            releaseAudioMute()

            Log.e(
                TAG,
                "Failed to draw overlay window",
                e
            )
        }
    }

    private fun removeOverlay() {

        val view = overlayView
            ?: return

        try {

            windowManager?.removeView(view)

            Log.d(
                TAG,
                "Overlay removed successfully"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to remove overlay window",
                e
            )

        } finally {

            overlayView = null

            isOverlayShowing = false
        }
    }

    override fun onInterrupt() {

        Log.d(
            TAG,
            "Accessibility service interrupted"
        )

        handler.removeCallbacksAndMessages(null)

        isLeavingBlockedApp = false

        leaveCooldownUntil = 0L

        releaseAudioMute()

        removeOverlay()
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "Accessibility service destroyed"
        )

        handler.removeCallbacksAndMessages(null)

        isLeavingBlockedApp = false

        leaveCooldownUntil = 0L

        releaseAudioMute()

        removeOverlay()

        super.onDestroy()
    }
}