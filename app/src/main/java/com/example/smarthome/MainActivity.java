package com.example.smarthome;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    BluetoothAdapter btAdapter;
    BluetoothSocket btSocket;
    OutputStream outputStream;
    String deviceAddress = "XX:XX:XX:XX:XX:XX"; // Replace with your ESP32 BT MAC
    UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.loadUrl("file:///android_asset/index.html");

        setupBluetooth();
    }

    void setupBluetooth() {
        try {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if(btAdapter == null){
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
                return;
            }
            if(!btAdapter.isEnabled()){
                btAdapter.enable();
            }
            BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            btSocket.connect();
            outputStream = btSocket.getOutputStream();
            Toast.makeText(this, "Bluetooth Connected", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Bluetooth Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void sendCommand(String cmd) {
            try {
                if(outputStream != null) {
                    outputStream.write(cmd.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if(btSocket != null) btSocket.close(); } catch (Exception e) {}
    }
}
