package com.example.ranjanasmarthome

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private lateinit var webView: WebView
    private lateinit var bleManager: SmartHomeBleManager

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

        // ================= BLE SETUP =================
        bleManager = SmartHomeBleManager(this)
        BLEController.init(bleManager)

        BLEController.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "BLE update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            WidgetState.update(l1, f1, l2, f2)
            updateWebView(l1, f1, l2, f2)
        }

        // ================= MQTT SETUP =================
        MQTTController.init(applicationContext)
        MQTTController.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "MQTT update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            WidgetState.update(l1, f1, l2, f2)
            updateWebView(l1, f1, l2, f2)
        }
    }

    /** Update the WebView UI with current device state */
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
        fun startBLE() {
            runOnUiThread { Toast.makeText(activity, "Starting BLE scan...", Toast.LENGTH_SHORT).show() }
            scanAndConnectBLE()
        }

        @android.webkit.JavascriptInterface
        fun sendBLE(cmd: String) {
            if (cmd.isNotBlank()) {
                BLEController.sendCommand(cmd)
                MQTTController.sendCommand(cmd)
            }
        }

        @android.webkit.JavascriptInterface
        fun connectBLE() {
            runOnUiThread { Toast.makeText(activity, "Connecting BLE...", Toast.LENGTH_SHORT).show() }
        }

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {
            BLEController.disconnect()
            runOnUiThread { Toast.makeText(activity, "BLE disconnected", Toast.LENGTH_SHORT).show() }
        }

        @android.webkit.JavascriptInterface
        fun setupBLE() {} // For HTML compatibility
    }

    // ================= BLE SCAN & CONNECT =================
    private fun scanAndConnectBLE() {
        val adapter = bleManager.bluetoothAdapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show()
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Toast.makeText(this, "BLE scanner not available", Toast.LENGTH_LONG).show()
            return
        }

        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                val device = result?.device ?: return
                val name = device.name ?: return
                if (name.startsWith("RanjanaSmartHome")) {
                    scanner.stopScan(this)
                    BLEController.connect(device)
                    runOnUiThread { Toast.makeText(this@MainActivity, "BLE Device Found: $name", Toast.LENGTH_SHORT).show() }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed: $errorCode")
            }
        }

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    // ================= CLEANUP =================
    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
    }
}
