package com.example.ranjanasmarthome

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class MainActivity : ComponentActivity() {

    private val TAG = "RanjanaBLE"

    // ---------------- WEBVIEW ----------------
    private lateinit var webView: WebView

    // ---------------- BLE UUIDs ----------------
    private val SERVICE_UUID =
        UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

    private val CHARACTERISTIC_TX =
        UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    private val CHARACTERISTIC_RX =
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    // ---------------- BLE ----------------
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private var targetDeviceAddress: String? = null

    private val handler =
        Handler(Looper.getMainLooper())

    // ---------------- BLE QUEUE ----------------
    private val bleCommandQueue:
            Queue<String> =
        ConcurrentLinkedQueue()

    private var isWriting = false

    // ---------------- PERMISSIONS ----------------
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val granted =
                result.entries.all { it.value }

            if (!granted) {

                Toast.makeText(
                    this,
                    "Bluetooth permissions required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // ---------------- FAST RECONNECT ----------------
    private val reconnectRunnable =
        object : Runnable {

            override fun run() {

                if (bluetoothGatt != null)
                    return

                try {

                    targetDeviceAddress?.let { addr ->

                        val device =
                            bluetoothAdapter
                                ?.getRemoteDevice(addr)

                        bluetoothGatt =
                            device?.connectGatt(
                                this@MainActivity,
                                false,
                                gattCallback
                            )
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Reconnect error",
                        e
                    )
                }

                handler.postDelayed(
                    this,
                    300
                )
            }
        }

    // =========================================================
    // ON CREATE
    // =========================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "JavascriptInterface"
    )
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Bluetooth
        val bluetoothManager =
            getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager

        bluetoothAdapter =
            bluetoothManager.adapter

        checkPermissions()

        // ---------------- WEBVIEW ----------------

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

            addJavascriptInterface(
                AndroidBridge(this@MainActivity),
                "Android"
            )

            loadUrl(
                "file:///android_asset/index.html"
            )
        }

        setContentView(webView)
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private fun checkPermissions() {

        val permissions =
            mutableListOf<String>()

        permissions.add(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }

        val missing =
            permissions.filter {

                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missing.isNotEmpty()) {

            permissionLauncher.launch(
                missing.toTypedArray()
            )
        }
    }

    // =========================================================
    // JAVASCRIPT BRIDGE
    // =========================================================

    inner class AndroidBridge(
        private val activity: Activity
    ) {

        @android.webkit.JavascriptInterface
        fun startBLE() {

            scanForDevices()
        }

        @android.webkit.JavascriptInterface
        fun connectBLE() {

            try {

                targetDeviceAddress?.let { addr ->

                    val device =
                        bluetoothAdapter
                            ?.getRemoteDevice(addr)

                    bluetoothGatt =
                        device?.connectGatt(
                            activity,
                            false,
                            gattCallback
                        )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Connect error",
                    e
                )
            }
        }

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {

            handler.removeCallbacks(
                reconnectRunnable
            )

            try {

                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()

            } catch (_: Exception) {
            }

            bluetoothGatt = null

            rxCharacteristic = null
            txCharacteristic = null

            bleCommandQueue.clear()

            isWriting = false

            notifyJS(
                "bleStatus",
                "Disconnected"
            )
        }

        @android.webkit.JavascriptInterface
        fun reconnectBLE() {

            handler.removeCallbacks(
                reconnectRunnable
            )

            handler.post(
                reconnectRunnable
            )
        }

        @android.webkit.JavascriptInterface
        fun sendBLE(cmd: String) {

            if (cmd.isBlank())
                return

            bleCommandQueue.add(cmd)

            processQueue()
        }

        // compatibility with your HTML
        @android.webkit.JavascriptInterface
        fun setupBLE() {
        }
    }

    // =========================================================
    // BLE SCAN
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun scanForDevices() {

        val scanner =
            bluetoothAdapter
                ?.bluetoothLeScanner
                ?: return

        notifyJS(
            "bleStatus",
            "Scanning..."
        )

        val callback =
            object : ScanCallback() {

                override fun onScanResult(
                    callbackType: Int,
                    result: ScanResult?
                ) {

                    val device =
                        result?.device ?: return

                    val name =
                        device.name ?: return

                    if (
                        name.startsWith(
                            "RanjanaSmartHome"
                        )
                    ) {

                        targetDeviceAddress =
                            device.address

                        scanner.stopScan(this)

                        notifyJS(
                            "bleFound",
                            name
                        )

                        Log.d(
                            TAG,
                            "Found: $name"
                        )
                    }
                }
            }

        val settings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        scanner.startScan(
            null,
            settings,
            callback
        )
    }

    // =========================================================
    // GATT CALLBACK
    // =========================================================

    private val gattCallback =
        object : BluetoothGattCallback() {

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (
                    newState ==
                    BluetoothProfile.STATE_CONNECTED
                ) {

                    Log.d(
                        TAG,
                        "Connected"
                    )

                    notifyJS(
                        "bleStatus",
                        "Connected"
                    )

                    handler.removeCallbacks(
                        reconnectRunnable
                    )

                    gatt.discoverServices()
                }

                else if (
                    newState ==
                    BluetoothProfile.STATE_DISCONNECTED
                ) {

                    Log.d(
                        TAG,
                        "Disconnected"
                    )

                    notifyJS(
                        "bleStatus",
                        "Disconnected"
                    )

                    try {

                        bluetoothGatt?.close()

                    } catch (_: Exception) {
                    }

                    bluetoothGatt = null

                    rxCharacteristic = null
                    txCharacteristic = null

                    handler.postDelayed(
                        reconnectRunnable,
                        300
                    )
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                val service =
                    gatt.getService(
                        SERVICE_UUID
                    ) ?: return

                rxCharacteristic =
                    service.getCharacteristic(
                        CHARACTERISTIC_RX
                    )

                txCharacteristic =
                    service.getCharacteristic(
                        CHARACTERISTIC_TX
                    )

                // Enable notifications

                gatt.setCharacteristicNotification(
                    txCharacteristic,
                    true
                )

                val descriptor =
                    txCharacteristic
                        ?.getDescriptor(
                            UUID.fromString(
                                "00002902-0000-1000-8000-00805f9b34fb"
                            )
                        )

                descriptor?.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                if (descriptor != null) {

                    gatt.writeDescriptor(
                        descriptor
                    )
                }

                notifyJS(
                    "bleStatus",
                    "Ready"
                )

                processQueue()
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {

                val msg =
                    characteristic.value
                        ?.toString(Charsets.UTF_8)
                        ?: return

                Log.d(
                    TAG,
                    "RX: $msg"
                )

                notifyJS(
                    "bleData",
                    msg
                )
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {

                isWriting = false

                processQueue()
            }
        }

    // =========================================================
    // BLE QUEUE
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun processQueue() {

        if (isWriting)
            return

        val cmd =
            bleCommandQueue.poll()
                ?: return

        val ch =
            rxCharacteristic
                ?: return

        try {

            ch.value =
                cmd.toByteArray()

            ch.writeType =
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            isWriting = true

            bluetoothGatt
                ?.writeCharacteristic(ch)

            Log.d(
                TAG,
                "TX: $cmd"
            )

        } catch (e: Exception) {

            isWriting = false

            Log.e(
                TAG,
                "Write failed",
                e
            )
        }
    }

    // =========================================================
    // SEND EVENTS TO HTML
    // =========================================================

    private fun notifyJS(
        event: String,
        data: String
    ) {

        runOnUiThread {

            when (event) {

                // ---------------- STATUS ----------------

                "bleStatus" -> {

                    webView.evaluateJavascript(
                        "onBLEStatus('$data');",
                        null
                    )
                }

                // ---------------- BLE DATA ----------------

                "bleData" -> {

                    val escaped =
                        data
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")

                    webView.evaluateJavascript(
                        "onBLEData('$escaped');",
                        null
                    )
                }

                // ---------------- DEVICE FOUND ----------------

                "bleFound" -> {

                    webView.evaluateJavascript(
                        "connectBLE();",
                        null
                    )
                }
            }
        }
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {

        super.onDestroy()

        try {

            handler.removeCallbacksAndMessages(
                null
            )

            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()

        } catch (_: Exception) {
        }
    }
}
