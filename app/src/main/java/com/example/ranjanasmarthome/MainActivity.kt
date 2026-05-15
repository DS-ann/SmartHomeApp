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

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                startSmartHomeService()
                initControllers()
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
            initControllers()
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }

        // ===================== OBSERVE WIDGETSTATE =====================
        WidgetState.onStateUpdate = { l1, f1, l2, f2 ->
            updateWebViewPartial(l1, f1, l2, f2) // Widget only
        }

        // Observe MQTT/BLE full relay + fan updates for WebView
        MQTTController.onStateUpdate = { l1, f1, l2, f2 ->
            updateWebViewFull(MQTTController.getRelayStates(), MQTTController.getFanSpeeds())
        }
        BLEController.onStateUpdate = { l1, f1, l2, f2 ->
            updateWebViewFull(BLEController.getRelayStates(), BLEController.getFanSpeeds())
        }
    }

    private fun allPermissionsGranted(): Boolean =
        REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

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

    private fun initControllers() {
        val bleManager = SmartHomeBleManager(this)
        SmartHomeService.setBleManager(bleManager)
        BLEController.init(bleManager)

        MQTTController.init(this)
        SmartHomeService.setMqttController(MQTTController)

        bleManager.connectSavedDevice()
        MQTTController.connect()
    }

    // ---------------- WEBVIEW UPDATE ----------------

    // Only for first 2 relays (Widget)
    private fun updateWebViewPartial(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        runOnUiThread {
            val l1 = if (light1) 1 else 0
            val f1 = fan1
            val l2 = if (light2) 1 else 0
            val f2 = fan2
            webView.evaluateJavascript("updateWidget($l1,$f1,$l2,$f2);", null)
        }
    }

    // Full update for WebView: all 8 relays + 2 fans
    private fun updateWebViewFull(relays: BooleanArray, fans: IntArray) {
        runOnUiThread {
            val relayStr = relays.joinToString(",") { if (it) "1" else "0" }
            val fanStr = fans.joinToString(",")
            webView.evaluateJavascript("updateAllRelaysAndFans([$relayStr],[$fanStr]);", null)
        }
    }

    inner class AndroidBridge {
        @android.webkit.JavascriptInterface
        fun sendRelay(relayNumber: String, state: Boolean) {
            SmartHomeService.sendRelayCommand(relayNumber, state)
        }

        @android.webkit.JavascriptInterface
        fun startBLE() {
            Log.d(TAG, "JS requested BLE start")
        }

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {
            SmartHomeBleManager.disconnectAll()
        }

        @android.webkit.JavascriptInterface
        fun connectBLE() {
            Log.d(TAG, "JS requested BLE connect")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartHomeBleManager.disconnectAll()
        MQTTController.disconnect()
    }
}
