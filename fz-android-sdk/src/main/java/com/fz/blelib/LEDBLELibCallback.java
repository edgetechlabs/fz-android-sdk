package com.fz.blelib;

import android.bluetooth.BluetoothGattService;

import java.util.List;

/**
 * Created by Zheng on 9/12/2016.
 */
public interface LEDBLELibCallback {
    public void onConnected();
    public void onDisconnected();
    public void onServiceDiscovered(List<BluetoothGattService> serviceList);
    public void onDataReceived(byte[] rxBytes);

    public void onBatteryString(String value);
    public void onManufactureNameString(String value);
    public void onModelNumberString(String value);
    public void onSerialNumberString(String value);
    public void onHardwareRevisionString(String value);
}
