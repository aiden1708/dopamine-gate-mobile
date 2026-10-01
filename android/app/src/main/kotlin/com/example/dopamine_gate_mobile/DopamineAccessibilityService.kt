package com.example.dopamine_gate_mobile

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
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

        // Prevent rapid duplicate processing while a blocked app is loading.
        private const val DEBOUNCE_MS = 800L
    }

    private var windowManager: WindowManager? = null

    private var overlayView: View? = null

    private var isOverlayShowing = false

    /*
     * True while we are deliberately leaving a blocked application
     * and returning to Dopamine Gate.
     *
     * This prevents intermediate Accessibility events from causing
     * another Gate during the transition.
     */
    private var isLeavingBlockedApp = false

    private var lastTriggerTime = 0L

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

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

        /*
         * Dopamine Gate itself has become the foreground application.
         *
         * If we were in the middle of leaving a blocked application,
         * this means the redirect has completed successfully.
         */
        if (packageName == applicationContext.packageName) {

            if (isLeavingBlockedApp) {
                Log.d(TAG, "Dopamine Gate is now foreground — transition completed")

                isLeavingBlockedApp = false
            }

            return
        }

        /*
         * If the user has chosen to leave the blocked app,
         * ignore all intermediate window events while Android
         * processes the navigation sequence.
         */
        if (isLeavingBlockedApp) {
            Log.d(TAG, "Leaving blocked app — ignoring event from $packageName")
            return
        }

        /*
         * Once the overlay is visible, keep it visible.
         *
         * Blocked apps can generate several window events while
         * loading or transitioning. Removing the overlay in response
         * to those events would cause flickering or expose the app
         * underneath the Gate.
         */
        if (isOverlayShowing) {
            return
        }

        /*
         * Ignore applications that are not protected by Dopamine Gate.
         */
        if (!ProtectedApps.packages.contains(packageName)) {
            return
        }

        /*
         * During an allowed time window, blocked apps can be used
         * normally without showing the Gate.
         */
        if (isWithinAllowedWindow()) {
            Log.d(TAG, "Allowed time — $packageName will not be blocked")
            return
        }

        val currentTime = System.currentTimeMillis()

        /*
         * Prevent repeated Accessibility events from immediately
         * creating another Gate.
         */
        if (currentTime - lastTriggerTime < DEBOUNCE_MS) {
            Log.d(TAG, "Ignoring duplicate blocked-app event: $packageName")
            return
        }

        lastTriggerTime = currentTime

        Log.d(TAG, "BLOCKED APP DETECTED: $packageName")

        showOverlay(packageName)
    }

    private fun isWithinAllowedWindow(): Boolean {
        val calendar = Calendar.getInstance()

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return GateSchedule.allowedWindows.any { window -> window.contains(currentMinutes) }
    }

    private fun showOverlay(blockedPackage: String) {

        /*
         * Absolute single-instance protection.
         */
        if (isOverlayShowing || overlayView != null) {
            Log.d(TAG, "Overlay already exists — not creating another")
            return
        }

        val appName = ProtectedApps.getAppName(blockedPackage)

        val overlayType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
                }

        val layoutParams =
                WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                overlayType,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                                PixelFormat.TRANSLUCENT
                        )
                        .apply { gravity = Gravity.CENTER }

        val container =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL

                    gravity = Gravity.CENTER

                    setBackgroundColor(Color.parseColor("#121212"))

                    setPadding(60, 60, 60, 60)

                    /*
                     * Consume touch events so the blocked application
                     * underneath cannot be interacted with.
                     */
                    isClickable = true
                    isFocusable = false
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
                    text = "Take a breath.\n\n" + "Do you really need to open $appName right now?"

                    textSize = 16f

                    setTextColor(Color.LTGRAY)

                    gravity = Gravity.CENTER

                    setPadding(0, 30, 0, 50)
                }

        val exitButton =
                Button(this).apply {
                    text = "Go to Dopamine Gate"

                    setBackgroundColor(Color.parseColor("#6200EE"))

                    setTextColor(Color.WHITE)

                    setPadding(40, 20, 40, 20)

                    setOnClickListener {

                        /*
                         * Prevent multiple taps from executing
                         * the navigation sequence more than once.
                         */
                        if (isLeavingBlockedApp) {
                            return@setOnClickListener
                        }

                        isLeavingBlockedApp = true

                        Log.d(TAG, "User chose Go to Dopamine Gate — " + "leaving $appName")

                        /*
                         * 1. Navigate backward through the blocked app.
                         *
                         * This does not force-stop the application process.
                         * It simply performs Android Back navigation.
                         */
                        performGlobalAction(GLOBAL_ACTION_BACK)

                        Log.d(TAG, "First GLOBAL_ACTION_BACK executed")

                        /*
                         * 2. Perform a second Back action after a
                         * short delay to clear deeper navigation state.
                         */
                        handler.postDelayed(
                                {
                                    performGlobalAction(GLOBAL_ACTION_BACK)

                                    Log.d(TAG, "Second GLOBAL_ACTION_BACK executed")
                                },
                                100L
                        )

                        /*
                         * 3. Launch Dopamine Gate's MainActivity.
                         *
                         * NEW_TASK is required because the service is
                         * not an Activity context.
                         */
                        handler.postDelayed(
                                {
                                    val intent =
                                            Intent(applicationContext, MainActivity::class.java)
                                                    .apply {
                                                        putExtra("fromBlockedApp", true)

                                                        addFlags(
                                                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                                        )
                                                    }

                                    applicationContext.startActivity(intent)

                                    Log.d(TAG, "Dopamine Gate MainActivity launched")

                                    /*
                                     * 4. Remove the enforcement overlay.
                                     *
                                     * We do NOT reset isLeavingBlockedApp here.
                                     * The AccessibilityService will reset it when
                                     * Dopamine Gate actually becomes foreground.
                                     */
                                    handler.postDelayed(
                                            {
                                                removeOverlay()

                                                Log.d(
                                                        TAG,
                                                        "Overlay removed — waiting for " +
                                                                "Dopamine Gate to become foreground"
                                                )
                                            },
                                            200L
                                    )
                                },
                                200L
                        )
                    }
                }

        container.addView(title)

        container.addView(subtitle)

        container.addView(exitButton)

        try {

            windowManager?.addView(container, layoutParams)

            overlayView = container

            isOverlayShowing = true

            Log.d(TAG, "Overlay screen drawn successfully")
        } catch (e: Exception) {

            overlayView = null

            isOverlayShowing = false

            Log.e(TAG, "Failed to draw overlay window", e)
        }
    }

    private fun removeOverlay() {

        val view = overlayView ?: return

        try {

            windowManager?.removeView(view)

            Log.d(TAG, "Overlay removed successfully")
        } catch (e: Exception) {

            Log.e(TAG, "Failed to remove overlay window", e)
        } finally {

            overlayView = null

            isOverlayShowing = false
        }
    }

    override fun onInterrupt() {

        Log.d(TAG, "Accessibility service interrupted")

        /*
         * Cancel any pending navigation callbacks.
         */
        handler.removeCallbacksAndMessages(null)

        isLeavingBlockedApp = false

        removeOverlay()
    }

    override fun onDestroy() {

        Log.d(TAG, "Accessibility service destroyed")

        handler.removeCallbacksAndMessages(null)

        isLeavingBlockedApp = false

        removeOverlay()

        super.onDestroy()
    }
}
