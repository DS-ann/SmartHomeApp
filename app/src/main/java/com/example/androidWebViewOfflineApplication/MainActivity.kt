package com.example.ranjanasmarthome

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
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

    // ESP32 BLE UUIDs
    private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var targetDeviceAddress: String? = null

    private lateinit var webView: WebView

    // BLE command queue
    private val bleCommandQueue: Queue<String> = ConcurrentLinkedQueue()
    private var isWriting = false
    private val handler = Handler(Looper.getMainLooper())

    // ---------------- Runtime Permissions ----------------
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (!granted) {
                Toast.makeText(this, "BLE & Location permissions are required", Toast.LENGTH_SHORT).show()
            }
        }

    // ---------------- Fast Reconnect Runnable ----------------
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            targetDeviceAddress?.let { addr ->
                val device = bluetoothAdapter?.getRemoteDevice(addr)
                bluetoothGatt = device?.connectGatt(this@MainActivity, false, gattCallback)
            }
            handler.postDelayed(this, 300) // Retry every 300ms until connected
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Bluetooth adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Request runtime permissions
        checkAndRequestPermissions()

        // Initialize WebView
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            WebView.setWebContentsDebuggingEnabled(true)
            addJavascriptInterface(AndroidBridge(this@MainActivity), "Android")
            loadUrl("file:///android_asset/index.html")
        }

        setContentView(webView)
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissionsLauncher.launch(missing.toTypedArray())
    }

    // ---------------- JavaScript Bridge ----------------
    inner class AndroidBridge(private val activity: Activity) {

        @android.webkit.JavascriptInterface
        fun startBLE() = scanForDevices()

        @android.webkit.JavascriptInterface
        fun connectBLE() {
            targetDeviceAddress?.let { addr ->
                val device = bluetoothAdapter?.getRemoteDevice(addr)
                bluetoothGatt = device?.connectGatt(activity, false, gattCallback)
            }
        }

        @android.webkit.JavascriptInterface
        fun disconnectBLE() {
            handler.removeCallbacks(reconnectRunnable)
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            rxCharacteristic = null
            txCharacteristic = null
            bleCommandQueue.clear()
            isWriting = false
        }

        @android.webkit.JavascriptInterface
        fun sendBLE(cmd: String) {
            bleCommandQueue.add(cmd)
            processQueue()
        }

        @android.webkit.JavascriptInterface
        fun reconnectBLE() {
            handler.removeCallbacks(reconnectRunnable)
            handler.post(reconnectRunnable)
        }
    }

    // ---------------- BLE Scan ----------------
    private fun scanForDevices() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    if (device.name?.startsWith("RanjanaSmartHome") == true) {
                        targetDeviceAddress = device.address
                        scanner.stopScan(this)
                        notifyJS("bleFound", "{\"name\":\"${device.name}\",\"address\":\"${device.address}\"}")
                        Log.d(TAG, "Target device found: $targetDeviceAddress")
                    }
                }
            }
        }
        val scanFilter = ScanFilter.Builder().setDeviceName("RanjanaSmartHome").build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(scanFilter), settings, scanCallback)
    }

    // ---------------- BLE GATT Callback ----------------
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val connected = newState == BluetoothProfile.STATE_CONNECTED
            notifyJS("bleStatus", "{\"status\":\"${if (connected) "connected" else "disconnected"}\"}")

            if (connected) {
                handler.removeCallbacks(reconnectRunnable)
                gatt.discoverServices()
            } else {
                // Auto fast reconnect
                if (targetDeviceAddress != null) handler.postDelayed(reconnectRunnable, 300)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            gatt.getService(SERVICE_UUID)?.let { service ->
                rxCharacteristic = service.getCharacteristic(CHARACTERISTIC_RX)
                txCharacteristic = service.getCharacteristic(CHARACTERISTIC_TX)
                gatt.setCharacteristicNotification(txCharacteristic, true)
                txCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))?.let { descriptor ->
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value.toString(Charsets.UTF_8)
            notifyJS("bleData", "{\"data\":\"$value\"}")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            isWriting = false
            processQueue()
        }
    }

    // ---------------- BLE Queue Processor ----------------
    private fun processQueue() {
        if (isWriting) return
        val cmd = bleCommandQueue.poll() ?: return
        rxCharacteristic?.let {
            it.value = cmd.toByteArray()
            isWriting = true
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    // ---------------- Notify JS ----------------
    private fun notifyJS(event: String, json: String) {
        runOnUiThread {
            webView.evaluateJavascript("window.onBLEEvent && window.onBLEEvent('$event', $json);", null)
        }
    }
}
