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
                initApp()
            } else {
                Toast.makeText(this, "BLE permissions denied. App cannot run.", Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions first
        if (allPermissionsGranted()) {
            initApp()
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    /** Initialize WebView + start service safely after permissions granted */
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initApp() {
        // WebView setup
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            WebView.setWebContentsDebuggingEnabled(true)
            addJavascriptInterface(AndroidBridge(this@MainActivity), "Android")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)

        // Start foreground service
        val intent = Intent(this, SmartHomeService::class.java)
        startForegroundService(intent)
        Toast.makeText(this, "Smart Home Service started", Toast.LENGTH_SHORT).show()

        // Observe WidgetState
        WidgetState.onStateUpdate = { l1, f1, l2, f2 ->
            updateWebView(l1, f1, l2, f2)
        }
    }

    private fun allPermissionsGranted(): Boolean =
        REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    /** Update WebView safely */
    private fun updateWebView(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        runOnUiThread {
            val l1 = if (light1) 1 else 0
            val l2 = if (light2) 1 else 0
            webView.evaluateJavascript("updateWidget($l1,$fan1,$l2,$fan2);", null)
        }
    }

    /** JS bridge with reference to Activity for safe UI calls */
    inner class AndroidBridge(private val activity: MainActivity) {

        @android.webkit.JavascriptInterface
        fun sendBLE(cmd: String) {
            if (cmd.isNotBlank()) {
                BLEController.sendCommand(cmd)
                MQTTController.sendCommand(cmd)
            }
        }

        @android.webkit.JavascriptInterface
        fun startBLE() {
            // BLE scanning is handled by the Service
        }

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {
            BLEController.disconnect()
            activity.runOnUiThread {
                Toast.makeText(activity, "BLE disconnected", Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun connectBLE() {
            activity.runOnUiThread {
                Toast.makeText(activity, "Connecting BLE...", Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun setupBLE() {} // HTML compatibility
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
    }
}
