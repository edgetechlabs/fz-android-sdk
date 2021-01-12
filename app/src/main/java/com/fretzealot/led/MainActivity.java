package com.fretzealot.led;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.fz.blelib.LEDBLELib;
import com.fz.blelib.LEDBLELibCallback;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 101;
    private static final int PERMISSION_CODE = 102;
    LEDBLELib ledbleLib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ledbleLib = LEDBLELib.getInstance(getApplicationContext());
        if (ledbleLib.isSupported()) {
            if (ledbleLib.isEnabled()) {
                checkForPermissions();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            checkForPermissions();
        }
    }

    private void checkForPermissions() {
        if (hasPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            startScanner();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}
                    , PERMISSION_CODE);
        }
    }

    private boolean hasPermissions(String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission required to connect fretzealot", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            startScanner();
        }
    }

    private void startScanner() {
        ledbleLib.startScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                if (bluetoothDevice != null && bluetoothDevice.getName() != null && bluetoothDevice.getName().toLowerCase().equals("fret zealot")) {
                    ledbleLib.stopScan();
                    startConnectionService(bluetoothDevice.getAddress());
                }
            }
        });
    }

    private void startConnectionService(String address) {
        ledbleLib.startService(address, new LEDBLELibCallback() {
            @Override
            public void onConnected() {
                Toast.makeText(MainActivity.this, "Fret zealot connected", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDisconnected() {
                Toast.makeText(MainActivity.this, "Fret zealot disconnected", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onServiceDiscovered(List<BluetoothGattService> serviceList) {

            }

            @Override
            public void onDataReceived(byte[] rxBytes) {

            }

            @Override
            public void onBatteryString(String value) {

            }

            @Override
            public void onManufactureNameString(String value) {

            }

            @Override
            public void onModelNumberString(String value) {

            }

            @Override
            public void onSerialNumberString(String value) {

            }

            @Override
            public void onHardwareRevisionString(String value) {

            }
        });
        ledbleLib.onResume();
    }

}
