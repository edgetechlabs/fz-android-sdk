package com.fz.blelib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.fz.blelib.SampleGattAttributes.ORG_BLUETOOTH_CHARACTERISTIC_BATTERY;
import static com.fz.blelib.SampleGattAttributes.ORG_BLUETOOTH_CHARACTERISTIC_HARDWARE_REVISION_STRING;
import static com.fz.blelib.SampleGattAttributes.ORG_BLUETOOTH_CHARACTERISTIC_MANUFACTURER_NAME_STRING;
import static com.fz.blelib.SampleGattAttributes.ORG_BLUETOOTH_CHARACTERISTIC_MODEL_NUMBER_STRING;
import static com.fz.blelib.SampleGattAttributes.ORG_BLUETOOTH_CHARACTERISTIC_SERIAL_NUMBER_STRING;

/**
 * Created by Zheng on 9/12/2016.
 */
public class LEDBLELib {

    private static final long SCAN_PERIOD = 10000;
    public static final String TAG = "LEDBLELib";

    public static final int FadeNotActive = 0;
    public static final int FadeInShort = 1;
    public static final int FadeInLong = 2;
    public static final int FadeOutShort = 3;
    public static final int FadeOutLong = 4;

    private static final int COMMAND_SET = 0x00;
    private static final int COMMAND_SET_ALL = 0x01;
    private static final int COMMAND_SET_ACROSS = 0x02;
    private static final int COMMAND_SET_SUBSET = 0x03;
    private static final int COMMAND_CLEAR = 0x04;
    private static final int COMMAND_SET_DISPLAY = 0x08;

    private Context mContext;

    private boolean mScanning;
    private String mDeviceAddress = "";
    private boolean mServiceRunning = false;
    private boolean mConnected = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicLED;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicDFU;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicDFUSec;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicLEDNoti;
    private BluetoothGattCharacteristic mNotifyCharacteristic;


    private BluetoothGattCharacteristic bluetoothGattCharacteristicBattery;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicManufactureName;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicModelNumber;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicSerialNumber;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHardwareRevision;

    private Handler mHandler;
    Handler mSpeedHandler;

    BluetoothAdapter.LeScanCallback mScanCallback;
    LEDBLELibCallback mCallback;

    private boolean first = true;
    public static LEDBLELib sInstance;

    public static LEDBLELib getInstance(Context context)
    {
        if(sInstance == null)
            sInstance = new LEDBLELib(context);
        return sInstance;
    }
    public static LEDBLELib getInstance()
    {
        return sInstance;
    }

    public LEDBLELib(Context context)
    {
        mContext = context;
        mHandler = new Handler();
        mSpeedHandler = new Handler();
        mScanning = false;

        BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }
    public boolean isSupported(){return mBluetoothAdapter != null;}
    public boolean isScanning(){return mScanning;}
    public boolean isConnected(){return mConnected;}
    public boolean isEnabled(){return mBluetoothAdapter.isEnabled();}
    public boolean isLED(){return bluetoothGattCharacteristicLED!= null;}
    public boolean isDFU(){return bluetoothGattCharacteristicDFUSec!= null;}
    public void startScan(BluetoothAdapter.LeScanCallback scanCallback)
    {
        mScanCallback = scanCallback;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mScanCallback);
            }
        }, SCAN_PERIOD);
        mScanning = true;
        mBluetoothAdapter.startLeScan(mScanCallback);
    }

    public void stopScan()
    {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mScanCallback);
    }

    public void startService(String DeviceAddress,LEDBLELibCallback callback)
    {
        first = true;
        mCallback = callback;
        mDeviceAddress = DeviceAddress;
        mServiceRunning = true;
        Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopService()
    {
        if(mServiceRunning)
            mContext.unbindService(mServiceConnection);
        mServiceRunning = false;
        mBluetoothLeService = null;
    }

    public void connect(String DeviceAddress)
    {
        mDeviceAddress = DeviceAddress;
        mBluetoothLeService.connect(mDeviceAddress);
    }
    public void disconnect()
    {
        mBluetoothLeService.disconnect();
    }
    public void onResume()
    {
        mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public void onPause() {
        mContext.unregisterReceiver(mGattUpdateReceiver);
    }

    private void getCharacteristics(List<BluetoothGattService> gattServices)
    {
        UUID UUID_LED = UUID.fromString(SampleGattAttributes.LED_CH);
        UUID UUID_DFU = UUID.fromString(SampleGattAttributes.LED_DFU);
        UUID UUID_DFU_SEC = UUID.fromString(SampleGattAttributes.LED_DFU_SEC);
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                //Check if it is "HM_10"
                Log.e(TAG, "UUID = " + uuid);
                if(uuid.equals(SampleGattAttributes.LED_CH)) {
                    bluetoothGattCharacteristicLED = gattService.getCharacteristic(UUID_LED);
                }
                else if(uuid.equals(SampleGattAttributes.LED_DFU))
                {
                    bluetoothGattCharacteristicDFU = gattService.getCharacteristic(UUID_DFU);
                }
                else if(uuid.equals(SampleGattAttributes.LED_DFU_SEC))
                {
                    bluetoothGattCharacteristicDFUSec = gattService.getCharacteristic(UUID_DFU_SEC);
                }
                else if(uuid.equals(ORG_BLUETOOTH_CHARACTERISTIC_MANUFACTURER_NAME_STRING.toString()))
                {
                    bluetoothGattCharacteristicManufactureName = gattService.getCharacteristic(ORG_BLUETOOTH_CHARACTERISTIC_MANUFACTURER_NAME_STRING);
                }
                else if(uuid.equals(ORG_BLUETOOTH_CHARACTERISTIC_MODEL_NUMBER_STRING.toString()))
                {
                    bluetoothGattCharacteristicModelNumber = gattService.getCharacteristic(ORG_BLUETOOTH_CHARACTERISTIC_MODEL_NUMBER_STRING);
                }
                else if(uuid.equals(ORG_BLUETOOTH_CHARACTERISTIC_SERIAL_NUMBER_STRING.toString()))
                {
                    bluetoothGattCharacteristicSerialNumber = gattService.getCharacteristic(ORG_BLUETOOTH_CHARACTERISTIC_SERIAL_NUMBER_STRING);
                }
                else if(uuid.equals(ORG_BLUETOOTH_CHARACTERISTIC_HARDWARE_REVISION_STRING.toString()))
                {
                    bluetoothGattCharacteristicHardwareRevision = gattService.getCharacteristic(ORG_BLUETOOTH_CHARACTERISTIC_HARDWARE_REVISION_STRING);
                }
                else if(uuid.equals(ORG_BLUETOOTH_CHARACTERISTIC_BATTERY.toString()))
                {
                    bluetoothGattCharacteristicBattery = gattService.getCharacteristic(ORG_BLUETOOTH_CHARACTERISTIC_BATTERY);
                }
                else if (uuid.equals(SampleGattAttributes.LED_CH_NOTI)) {
                    bluetoothGattCharacteristicLEDNoti = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.LED_CH_NOTI));
                    final int charaProp = bluetoothGattCharacteristicLEDNoti.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristicLEDNoti);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = bluetoothGattCharacteristicLEDNoti;
                        mBluetoothLeService.setCharacteristicNotification(
                                bluetoothGattCharacteristicLEDNoti, true);
                    }
                }
            }
        }
        if(bluetoothGattCharacteristicLED == null)
        {

        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                bluetoothGattCharacteristicLED = null;
                bluetoothGattCharacteristicLEDNoti = null;
                bluetoothGattCharacteristicDFUSec = null;
                bluetoothGattCharacteristicDFU = null;
                mCallback.onConnected();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                bluetoothGattCharacteristicLED = null;
                bluetoothGattCharacteristicLEDNoti = null;
                bluetoothGattCharacteristicDFUSec = null;
                bluetoothGattCharacteristicDFU = null;
                mCallback.onDisconnected();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                getCharacteristics(mBluetoothLeService.getSupportedGattServices());
                mCallback.onServiceDiscovered(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                if(bluetoothGattCharacteristicLEDNoti != null)
                {
                    final byte[] rxBytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);//bluetoothGattCharacteristicHM_10.getValue();

                    if (rxBytes == null)
                        return;

                    if(first)
                    {
                        first = false;
                        if(rxBytes.length == 1 && rxBytes[0] == 0)
                            return;
                    }

                    String UUID = intent.getStringExtra("UUID");
                    if(UUID.equals(ORG_BLUETOOTH_CHARACTERISTIC_BATTERY.toString()))
                    {
                        String value = String.valueOf(rxBytes[0]);
                        mCallback.onBatteryString(value);
                    }
                    else if(UUID.equals(ORG_BLUETOOTH_CHARACTERISTIC_MANUFACTURER_NAME_STRING.toString()))
                    {
                        String value = new String(rxBytes);
                        mCallback.onManufactureNameString(value);
                    }
                    else if(UUID.equals(ORG_BLUETOOTH_CHARACTERISTIC_MODEL_NUMBER_STRING.toString()))
                    {
                        String value = new String(rxBytes);
                        mCallback.onModelNumberString(value);
                    }
                    else if(UUID.equals(ORG_BLUETOOTH_CHARACTERISTIC_SERIAL_NUMBER_STRING.toString()))
                    {
                        String value = new String(rxBytes);
                        mCallback.onSerialNumberString(value);
                    }
                    else if(UUID.equals(ORG_BLUETOOTH_CHARACTERISTIC_HARDWARE_REVISION_STRING.toString()))
                    {
                        String value = new String(rxBytes);
                        mCallback.onHardwareRevisionString(value);
                    }
                    else {
                        mCallback.onDataReceived(rxBytes);
                    }
                }
            }
            else if (BluetoothLeService.ACTION_ON_WRITE.equals(action)) {
                if(sendIndex <sendBuffer.size()) {
                    if (sendIndex + 20 < sendBuffer.size()) {
                        byte[] data = new byte[20];
                        for (int i = 0; i < 20; i++)
                            data[i] = (byte) sendBuffer.get(sendIndex + i);
                        sendData(data);
                        sendIndex += 20;
                    } else {
                        byte[] data = new byte[sendBuffer.size() - sendIndex];
                        for (int i = 0; i < sendBuffer.size() - sendIndex; i++)
                            data[i] = (byte) sendBuffer.get(sendIndex + i);
                        sendData(data);
                        sendIndex += sendBuffer.size() - sendIndex;
                    }
                }
            }
        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_ON_WRITE);
        return intentFilter;
    }
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                mBluetoothLeService = null;
                mServiceRunning = false;
                return;
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            bluetoothGattCharacteristicLED = null;
        }
    };

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String getDeviceAddress()
    {
        return mDeviceAddress;
    }
    public void sendDFUMode()
    {
        if(bluetoothGattCharacteristicDFU != null)
        {
            bluetoothGattCharacteristicDFU.setValue(new byte[]{(byte)0x01});
            Log.e(TAG, "go to DFU Mode");
            mBluetoothLeService.setCharacteristicDFUNotification(bluetoothGattCharacteristicDFU,true);
            android.os.SystemClock.sleep(500);
            mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicDFU);
        }
        else
        {
            Log.e(TAG, "Can't find character");
        }
    }

    private void sendData(byte[] txBytes)
    {
        if(bluetoothGattCharacteristicLED != null)
        {
            bluetoothGattCharacteristicLED.setValue(txBytes);
            Log.e(TAG, "Send Buffer : " + bytesToHex(txBytes));
            mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicLED);
            //mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicLEDNoti,true);
        }
        else
        {
            Log.e(TAG, "Can't find character");
        }
    }


    ArrayList sendBuffer = new ArrayList();
    private int SENDING_INTERVAL = 50;

    public void sendCommandBufferClear()
    {
        sendIndex = 0;
        sendBuffer.clear();
    }


   /* public void sendCommandFlush()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int index = 0;

                while(index <sendBuffer.size())
                {
                    if(index + 20 < sendBuffer.size())
                    {
                        byte[] data= new byte[20];
                        for(int i =0; i<20; i++)
                            data[i]=(byte)sendBuffer.get(index + i);
                        sendData(data);
                        index+=20;

                        try {
                            Thread.sleep(SENDING_INTERVAL);
                        }
                        catch (Exception ex)
                        {

                        }
                    }
                    else
                    {
                        byte[] data= new byte[sendBuffer.size() - index];
                        for(int i =0; i<sendBuffer.size() - index; i++)
                            data[i]=(byte)sendBuffer.get(index + i);
                        sendData(data);
                        index+=sendBuffer.size() - index;
                    }
                }

            }
        },10);

    } */

    int sendIndex = 0;
    public void sendCommandFlush()
    {
        if(20 < sendBuffer.size())
        {
            byte[] data= new byte[20];
            for(int i =0; i<20; i++)
                data[i]=(byte)sendBuffer.get(i);
            sendData(data);
            sendIndex+=20;
        }
        else
        {
            byte[] data= new byte[sendBuffer.size()];
            for(int i =0; i<sendBuffer.size(); i++)
                data[i]=(byte)sendBuffer.get(i);
            sendData(data);
            sendIndex += sendBuffer.size();
        }
    }


    public void set(byte strand, byte pixel, byte R, byte B, byte G, byte Intensity, byte fade_mode)
    {
        //sendData(new byte[]{strand, pixel, R,G,B,Intensity,fade_mode});
        addBuffer((byte)COMMAND_SET, fade_mode,strand,R,G,B,pixel);
    }

    public void set_all(byte R, byte B, byte G, byte Intensity, byte fade_mode)
    {
        //sendData(new byte[]{COMMAND_SET_ALL, R,G,B,Intensity,fade_mode});
        addBuffer((byte)COMMAND_SET_ALL, fade_mode,(byte)0,R,G,B,(byte)0);
    }

    public void set_across(byte pixel, byte R, byte B, byte G, byte Intensity, byte fade_mode)
    {
       // sendData(new byte[]{COMMAND_SET_ACROSS, pixel, R,G,B,Intensity,fade_mode});
        addBuffer((byte)COMMAND_SET_ACROSS, fade_mode,(byte)0,R,G,B,pixel);
    }

    public void set_subset(byte strand_start, byte pixel, byte R, byte B, byte G, byte Intensity, byte fade_mode)
    {
        //sendData(new byte[]{COMMAND_SET_SUBSET, strand_start, pixel, R,G,B,Intensity,fade_mode});
        addBuffer((byte)COMMAND_SET_SUBSET, fade_mode,strand_start,R,G,B,pixel);
    }

    public void clear()
    {
        //sendData(new byte[]{COMMAND_CLEAR});
        addBuffer((byte)COMMAND_CLEAR, (byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0);
    }

    public void set_display(byte strand_start, byte Intensity, byte fade_mode)
    {
        //sendData(new byte[]{COMMAND_SET_SUBSET, strand_start, pixel, R,G,B,Intensity,fade_mode});
        addBuffer((byte)COMMAND_SET_DISPLAY, fade_mode,strand_start,(byte)0,(byte)0,(byte)0,(byte)0);
    }

    public void addBuffer(byte cmd, byte fade, byte strand,  byte R, byte B, byte G, byte pixel)
    {
        byte d_1 = (byte)((cmd << 4) | (fade & 0x0f));
        byte d_2 = (byte)((strand <<4) | ((R & 0xf)));
        byte d_3 = (byte)((G & 0xF)<<4 | ((B & 0xf)));
        byte d_4 = (byte)(1<<(pixel+1));

        sendBuffer.add(d_1);
        sendBuffer.add(d_2);
        sendBuffer.add(d_3);
        sendBuffer.add(d_4);
        Log.e(TAG, "result" + String.valueOf(d_1) + " : "+ String.valueOf(d_2) + " : "+ String.valueOf(d_3) + " : "+ String.valueOf(d_4) + " : ");
    }


    public void readBattery()
    {
        if (bluetoothGattCharacteristicBattery == null) {
            Log.e(TAG, "current bluetoothGattCharacteristicBattery null");
            return;
        }
        mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristicBattery);
    }

    public void readManufacturerName()
    {
        if (bluetoothGattCharacteristicManufactureName == null) {
            Log.e(TAG, "current bluetoothGattCharacteristicBattery null");
            return;
        }
        mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristicManufactureName);
    }

    public void readModelNumberString()
    {
        if (bluetoothGattCharacteristicModelNumber == null) {
            Log.e(TAG, "current bluetoothGattCharacteristicBattery null");
            return;
        }
        mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristicModelNumber);
    }

    public void readSerialNumber()
    {
        if (bluetoothGattCharacteristicSerialNumber == null) {
            Log.e(TAG, "current bluetoothGattCharacteristicBattery null");
            return;
        }
        mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristicSerialNumber);
    }

    public void readHardwareRevision()
    {
        if (bluetoothGattCharacteristicHardwareRevision == null) {
            Log.e(TAG, "current bluetoothGattCharacteristicBattery null");
            return;
        }
        mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristicHardwareRevision);
    }

}
