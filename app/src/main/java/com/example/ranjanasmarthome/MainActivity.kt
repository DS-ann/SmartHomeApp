package com.example.ranjanasmarthome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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

        // ================= START FOREGROUND SERVICE =================
        val serviceIntent = Intent(this, SmartHomeService::class.java)
        startForegroundService(serviceIntent)

        // ================= OBSERVE STATE =================
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
                Toast.makeText(activity, "Starting BLE scan...", Toast.LENGTH_SHORT).show()
                // The service handles scanning in background, no direct scan here
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
