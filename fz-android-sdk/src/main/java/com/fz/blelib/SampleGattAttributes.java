/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fz.blelib;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String LED_CH = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";// "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String LED_CH_NOTI = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    public static String LED_DFU = "8f400001-f315-4f60-9fb8-838830daea51";
    public static String LED_DFU_SEC = "8ec90001-f315-4f60-9fb8-838830daea50";

    public static final UUID ORG_BLUETOOTH_SERVICE_DEVICE_INFORMATION              = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_MANUFACTURER_NAME_STRING = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_MODEL_NUMBER_STRING      = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");
    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_SERIAL_NUMBER_STRING     = UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb");
    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_HARDWARE_REVISION_STRING = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_FIRMWARE_REVISION_STRING = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");
    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_SOFTWARE_REVISION_STRING = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb");


    public static final UUID ORG_BLUETOOTH_CHARACTERISTIC_BATTERY = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "LED BLE Service"); //attributes.put("0000ffe0-0000-1000-8000-00805f9b34fb", "LED BLE Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(LED_CH, "LED Module");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
