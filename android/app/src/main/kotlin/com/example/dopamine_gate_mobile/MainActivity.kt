package com.example.dopamine_gate_mobile

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val PERMISSION_CHANNEL = "com.example.dopamine_gate/permissions"

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Handle permission checks from Flutter UI if needed
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            PERMISSION_CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "checkAccessibility" -> {
                    val enabled = PermissionHelper.isAccessibilityServiceEnabled(
                        this,
                        DopamineAccessibilityService::class.java
                    )
                    result.success(enabled)
                }
                "openAccessibility" -> {
                    PermissionHelper.openAccessibilitySettings(this)
                    result.success(true)
                }
                "checkBatteryOptimization" -> {
                    val isIgnoring = PermissionHelper.isIgnoringBatteryOptimizations(this)
                    result.success(isIgnoring)
                }
                "requestDisableBattery" -> {
                    PermissionHelper.requestDisableBatteryOptimization(this)
                    result.success(true)
                }
                "openMiuiPopupPermission" -> {
                    PermissionHelper.openMiuiPermissionScreen(this)
                    result.success(true)
                }
                "openMiuiAutostart" -> {
                    PermissionHelper.openMiuiAutostartScreen(this)
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}