package com.example.androidWebViewOfflineApplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private val applicationUrl =
        "file:///android_asset/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        webView.apply {

            // ---------- WEBVIEW CLIENT ----------
            webViewClient = WebViewClient()

            // ---------- CHROME CLIENT ----------
            webChromeClient = object : WebChromeClient() {

                // Microphone permission for voice recognition
                override fun onPermissionRequest(
                    request: PermissionRequest
                ) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        request.grant(request.resources)

                    }

                }

            }

            // ---------- SETTINGS ----------
            settings.apply {

                javaScriptEnabled = true

                domStorageEnabled = true

                allowFileAccess = true

                allowContentAccess = true

                databaseEnabled = true

                loadsImagesAutomatically = true

                mixedContentMode =
                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                mediaPlaybackRequiresUserGesture = false

                javaScriptCanOpenWindowsAutomatically = true

                setSupportMultipleWindows(false)

            }

            // ---------- PERFORMANCE ----------
            setLayerType(
                WebView.LAYER_TYPE_HARDWARE,
                null
            )

            // ---------- DEBUGGING ----------
            WebView.setWebContentsDebuggingEnabled(true)

            // ---------- LOAD APP ----------
            loadUrl(applicationUrl)

        }

        setContentView(webView)

    }

    // ---------- BACK BUTTON ----------
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        finish()

    }

}
