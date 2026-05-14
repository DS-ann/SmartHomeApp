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
    private val PERMISSIONS_REQUEST_CODE = 1001

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

        // ================= REQUEST PERMISSIONS =================
        requestPermissionsIfNeeded()
    }

    /** Request runtime permissions for BLE + location + foreground service */
    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        // Location required for BLE scanning on older Android
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        // Foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            startSmartHomeService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startSmartHomeService()
            } else {
                Toast.makeText(this, "Permissions are required for BLE operation", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Start foreground service that manages BLE + MQTT */
    private fun startSmartHomeService() {
        val serviceIntent = Intent(this, SmartHomeService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        // Observe state updates from service
        WidgetState.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "WidgetState update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            updateWebView(l1, f1, l2, f2)
        }
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
                Toast.makeText(activity, "BLE scanning handled by background service", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not stop the service; foreground service keeps BLE/MQTT alive
    }
}
