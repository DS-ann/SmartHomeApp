package com.example.ranjanasmarthome

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private lateinit var webView: WebView

    private val REQUEST_BLE_PERMISSIONS = 101

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
            add(Manifest.permission.ACCESS_FINE_LOCATION) // needed for BLE on older Android
        }
    }.toTypedArray()

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ================= WEBVIEW SETUP =================
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = false

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            WebView.setWebContentsDebuggingEnabled(true)

            addJavascriptInterface(AndroidBridge(this@MainActivity), "Android")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)

        // ================= PERMISSIONS =================
        if (allPermissionsGranted()) {
            startSmartHomeService()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_BLE_PERMISSIONS)
        }

        // ================= OBSERVE WIDGET STATE =================
        WidgetState.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "WidgetState update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            updateWebView(l1, f1, l2, f2)
        }
    }

    /** Check if all required permissions are granted */
    private fun allPermissionsGranted(): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Start the foreground service handling BLE + MQTT */
    private fun startSmartHomeService() {
        val serviceIntent = Intent(this, SmartHomeService::class.java)
        startForegroundService(serviceIntent)
        Toast.makeText(this, "Smart Home Service started", Toast.LENGTH_SHORT).show()
    }

    /** Update WebView UI safely */
    private fun updateWebView(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        runOnUiThread {
            val l1 = if (light1) 1 else 0
            val l2 = if (light2) 1 else 0
            webView.evaluateJavascript("updateWidget($l1, $fan1, $l2, $fan2);", null)
        }
    }

    // ================= JS BRIDGE =================
    inner class AndroidBridge(private val activity: Activity) {

        @android.webkit.JavascriptInterface
        fun sendBLE(cmd: String) {
            if (cmd.isNotBlank()) {
                BLEController.sendCommand(cmd)
                MQTTController.sendCommand(cmd)
            }
        }

        @android.webkit.JavascriptInterface
        fun setupBLE() {} // For HTML compatibility

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {
            BLEController.disconnect()
            runOnUiThread {
                Toast.makeText(activity, "BLE disconnected", Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun connectBLE() {
            runOnUiThread {
                Toast.makeText(activity, "Connecting BLE...", Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun startBLE() {
            runOnUiThread {
                Toast.makeText(activity, "Starting BLE scan in background...", Toast.LENGTH_SHORT).show()
                // The service handles scanning in background
            }
        }
    }

    // ================= PERMISSIONS CALLBACK =================
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startSmartHomeService()
            } else {
                Toast.makeText(this, "BLE permissions denied. App cannot run properly.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect UI-level BLE only if needed; service keeps running
        BLEController.disconnect()
        MQTTController.disconnect()
    }
}
