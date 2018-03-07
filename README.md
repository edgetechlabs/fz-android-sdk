<h1>BLE Library</h1>

<h2> Integration </h2>
<p>In order to integrate the BLE library, it can be imported as a module in Android Studio as follows:</p>

`File -> New -> Import module` and follow the steps shown by the wizard.


Under the ``` <application> ``` tag in your manifest, add the following lines

```java
        <service
            android:name="com.fz.LED.BluetoothLeService"
            android:enabled="true" >
        </service>
```
<h2> Usage </h2>
<p>While using the library for the first time in the app, a library instance should be created as follows: </p>

To integrate this BLE library in android application minimum **Android version 19** is required

   <p> **In the activity where the library is used for the first time in the application lifecycle. DO NOT call `finish();` in this activity since the library instance is created using its context.**  </p>

```java
     private LEDBLELib mLib;

     @Override
     protected void onCreate(Bundle savedInstanceState) {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            //BLE is not supported, library can't be used in the app


        } else {
            mLib = LEDBLELib.getInstance(this);
        }

     }
```


The next time the library is instantiated (*ideally in a different class*), it can be instantiated as follows:

 ```java
 mLib = LEDBLELib.getInstance();
 ```

<p> Following are the steps to initiate scanning and connecting to Fret Zealot </p>

```java
     mLib.startScan(mLeScanCallback);
     private BluetoothAdapter.LeScanCallback mLeScanCallback =
             new BluetoothAdapter.LeScanCallback() {

                 @Override
                 public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                     runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             if (!mLeDevices.contains(device)) {
                                 if (device.getName() != null) {
                                     if (device.getName().toLowerCase().contains("fret zealot")) {
                                         mLeDevices.add(device);
                                     }
                                 }
                                 if (device.getName() != null) {
                                     if (device.getName().toLowerCase().contains("fret zealot")) {
                                         deviceAdapter.notifyItemInserted(mLeDevices.size() - 1);

                                     }
                                 }
                             }
                             if (btEnabled) {
                                 hideProgressDialog();
                             }
                         }

                     });
                 }

```


And in order to connect to a device, the following method needs to be called:

 ```java
 mLib.connect(deviceAddress)
 ```

where **deviceAddress** is a `String`.

To start the service on Fret Zealot :

 ```java
     mLib.startService(mDeviceAddress, mLibCallback);
     protected LEDBLELibCallback mLibCallback = new LEDBLELibCallback() {
         @Override
         public void onConnected() {
             Toast.makeText(MainActivity.this, "Device connected", Toast.LENGTH_SHORT).show();
         }

         @Override
         public void onDisconnected() {
             try {
                 mLib.disconnect();
             } catch (Exception e) {
                 e.printStackTrace();
             }
             Toast.makeText(MainActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show();
         }

         @Override
         public void onServiceDiscovered(List<BluetoothGattService> serviceList) {
             try {

                 if (!mLib.isLED()) {
                     Toast.makeText(MainActivity.this, "Connected (Not LED)", Toast.LENGTH_SHORT).show();
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             }
         }

         @Override
         public void onDataReceived(byte[] rxBytes) {

         }

         @Override
         public void onBatteryString(String s) {

         }

         @Override
         public void onManufactureNameString(String s) {

         }

         @Override
         public void onModelNumberString(String s) {

         }

         @Override
         public void onSerialNumberString(String s) {

         }

         @Override
         public void onHardwareRevisionString(String s) {

         }
     };
```

Once the instance is ready, we can use the following methods of the library to send commands to Fret Zealot.
All the commands are held in a **command buffer**. This means that the commands are collected into a buffer prior to getting sent. A command buffer can hold a maximum of 5 commands. To insert a command into the buffer, various methods can be called as shown below.
*All the values sent as arguments should be converted to `byte` from `int`*

Also, the parameters commonly used are:

**fret** and **string** - These are self explanatory

**red, blue** and **green** - These are the values for the LED color. Possible values for each can be between 0 and 15.

**intensity** - The intensity of the LED. Possible values are between 0 and 10.
**fadeMode** - The fade effect with which ti light up the LED.
Possible values for **fadeMode** are between 0 and 4.

Fade effect reference chart:
```
Fade Mode Value          Description


     0                 Fade not active    //Set Pixel On
     1                 Fade in short      //Fade in Pixel over 50ms
     2                 Fade in long       //Fade out Pixel over 50ms
     3                 Fade out short     //Fade in Pixel over 200ms
     4                 Fade out long      //fade out Pixel over 200ms

```
There are several methods for controlling LED state on the fretboard

The `set` method - This method lights up a single LED

```java
mLib.set((byte)fret, (byte)string, (byte)red, (byte)blue, (byte)green, (byte)intensity, (byte)fadeMode);
```

The `set_across` method - This method lights up all the frets in a string
```java
mLib.set_across((byte)string, (byte)red, (byte)blue, (byte)green, (byte)intensity, (byte)fadeMode);
```

The `set_all` method - This method lights up the whole fretboard with the LED color supplied as arguments.
```java
mLib.set_all((byte)string, (byte)red, (byte)blue, (byte)green, (byte)intensity, (byte)fadeMode);
```

The `set_subset` method - This method lights up a string from a given fret to the 16th fret. For example, if you need to turn on the LEDs from fret 8 to fret 16, this is the method for you. However, the upper limit (16) cannot be changed.
```java
mLib.set_subset((byte)starting_fret, (byte)string, (byte)red, (byte)blue, (byte)green, (byte)intensity, (byte)fadeMode);
```

The `clear` method - This method sets each LED on the fretboard to **off**. This counts as one command of the 5 allowed in the **command buffer**
```java
mLib.clear();
```

There exist several utility methods for inteacting with the **command buffer**. The **command buffer** holds 5 (five) serialized commands,
and mirrors the size of the Bluetooth MTU. This structure does not need to be initialized.

The `sendCommandBufferClear` method - This method clears the buffer of any existing data, and should be invoked before each write to the fretboard.
```java
mLib.sendCommandBufferClear();
```

The `sendCommandFlush()` method - This method dumps the contents (up to 5 commands) of the **command buffer** to the BLE device
```java
mLib.sendCommandFlush();
```

Commands should be wrapped in these utility commands, to ensure delivery to the Fret Zealot
A typical command to set completely new pixels --  (C) major Triad in Standard Tuning
```java
mLib.sendCommandBufferClear();          // Flush the buffer
mLib.clear();                           //Clear all displayed Pixels
mLib.set((byte) 3, (byte) 4, (byte) 0, (byte) 15, (byte) 0, (byte) 9, (byte) 0);  // Set 'C' to blue
mLib.set((byte) 2, (byte) 3, (byte) 0, (byte) 0, (byte) 15, (byte) 9, (byte) 0);  // Set 'E' to green
mLib.set((byte) 0, (byte) 2, (byte) 15, (byte) 15, (byte) 15, (byte) 9, (byte) 0);  // Set 'G' to blue
mLib.sendCommandFlush();                // Write commands to the Fret Zealot

```
The `onResume()` method - This method reconnects the bluetooth connection when an activity is resumed.
```java
   @Override
     protected void onResume() {
          mLib.onResume();
          super.onResume();
     }

```
The `onPause()` method - This method disconnect the bluetooth connection when an activity is paused.
phase.
```java
@Override
    protected void onPause() {
            mLib.onPause();
            super.onPause();
    }

```

The `isConnected()` method - This method returns boolean value **true** if fretboard is connected with application and **false**
 if fretboard is not connected with application.
 ```java
  mLib.isConnected();

 ```
 <h2>Light Show </h2>
 The `set_display` method - This method takes **strand_start**, **Intensity** and **fade_mode** as parameter
 and light up on fretboard according to parameter fad value.

 **strand_start** :

 **intensity** : Lights intensity on fretboard

 **fade_mode**:
 ```
        fade_mode                      Description

           0                                    No lights
           1                                    Sparkler
           2                                    Bolt
           3                                    Rainbow
 ```
  ```java
    mLib.sendCommandBufferClear();
    mLib.set_display((byte) strand_start, (byte) intensity, (byte) fadValue);
    mLib.sendCommandFlush();
  ```

