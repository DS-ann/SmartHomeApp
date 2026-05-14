package com.example.ranjanasmarthome

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val TAG = "MainActivity"

    // Permissions required
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.INTERNET,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.FOREGROUND_SERVICE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    // Activity Result Launcher for permissions
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                startSmartHomeService()
            } else {
                Toast.makeText(this, "BLE permissions denied. App cannot run.", Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===================== WEBVIEW SETUP =====================
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            WebView.setWebContentsDebuggingEnabled(true)
            addJavascriptInterface(AndroidBridge(), "Android")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)

        // ===================== PERMISSIONS CHECK =====================
        if (allPermissionsGranted()) {
            startSmartHomeService()
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }

        // ===================== OBSERVE WIDGETSTATE =====================
        WidgetState.onStateUpdate = { l1, f1, l2, f2 ->
            updateWebView(l1, f1, l2, f2)
        }
    }

    // Check if all required permissions are granted
    private fun allPermissionsGranted(): Boolean =
        REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    // Start the foreground service safely
    private fun startSmartHomeService() {
        try {
            val intent = Intent(this, SmartHomeService::class.java)
            startForegroundService(intent)
            Toast.makeText(this, "Smart Home Service started", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Smart Home Service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            Toast.makeText(this, "Failed to start service: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Update WebView UI with WidgetState
    private fun updateWebView(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        runOnUiThread {
            val l1 = if (light1) 1 else 0
            val l2 = if (light2) 1 else 0
            webView.evaluateJavascript("updateWidget($l1,$fan1,$l2,$fan2);", null)
        }
    }

    // ===================== JS BRIDGE =====================
    inner class AndroidBridge {

        @android.webkit.JavascriptInterface
        fun sendBLE(cmd: String) {
            if (cmd.isNotBlank()) {
                BLEController.sendCommand(cmd)
                MQTTController.sendCommand(cmd)
            }
        }

        @android.webkit.JavascriptInterface
        fun startBLE() {
            // Service handles scanning
        }

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {
            BLEController.disconnect()
        }

        @android.webkit.JavascriptInterface
        fun connectBLE() {
            // Optional: show toast or just log
            Log.d(TAG, "JS requested BLE connect")
        }

        @android.webkit.JavascriptInterface
        fun setupBLE() {} // For HTML compatibility
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
    }
}
