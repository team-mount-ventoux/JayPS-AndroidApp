package com.njackson.sensor;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes
 */
public class BLESampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    // 0X2A37 https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    // 0X2A5B https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.csc_measurement.xml
    public static String CSC_MEASUREMENT = "00002a5b-0000-1000-8000-00805f9b34fb";
    // 0x2a53 https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.rsc_measurement.xml
    public static String RSC_MEASUREMENT = "00002a53-0000-1000-8000-00805f9b34fb";

    // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.battery_level.xml
    public static String TEMPERATURE_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb";

    //https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.temperature_measurement.xml
    public static String BATTERY_LEVEL = "00002A19-0000-1000-8000-00805f9b34fb";

    //https://github.com/dobos/LightRemote/blob/master/src/LightRemote/Constants.cs
    public static String LIGHT_MODE_SERVICE = "71261000-3692-ae93-e711-472ba41689c9";
    public static String LIGHT_MODE = "71261001-3692-ae93-e711-472ba41689c9";
    //https://github.com/dobos/LightRemote/blob/master/src/LightRemote/Assets/Config.xml
    public static String LIGHT_MODES_JSON = "{\"Flare RT\": {\"Off\": 0, \"Day Steady\": 1, \"Night Steady\": 5, \"Day Flash\": 7, \"All Day Flash\": 8, \"Night Flash\": 63}, \"ION PRO RT\": {\"Off\": 0, \"High\": 1, \"Medium\": 2, \"Low\": 5, \"Night Flash\": 62, \"Day Flash\": 63}, \"ION 200 RT\": {\"Off\": 0, \"High\": 1, \"Medium\": 2, \"Low\": 5, \"Night Flash\": 62, \"Day Flash\": 63}}";

    static {
        // Sample Services.
        // https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");
        attributes.put("00001814-0000-1000-8000-00805f9b34fb", "Running Speed and Cadence");
        attributes.put("00001816-0000-1000-8000-00805f9b34fb", "Cycling Speed and Cadence");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");

        // Sample Characteristics.
        // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
        //http://www.spinics.net/lists/linux-bluetooth/msg31630.html
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Device name");
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
        attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "Peripheral Preferred Connection Parameters");
        attributes.put("00002a1a-0000-1000-8000-00805f9b34fb", "Battery Power State characteristic UUID");
        attributes.put("00002a1b-0000-1000-8000-00805f9b34fb", "Battery Level State" );
        attributes.put("00002a1c-0000-1000-8000-00805f9b34fb", "Temperature Measurement");
        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level" );
        attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "System ID");
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "Model Number String");
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "Serial Number String");
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Revision String");
        attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "Hardware Revision String");
        attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "Software Revision String");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("00002a2a-0000-1000-8000-00805f9b34fb", "IEEE 11073-20601 Regulatory Certification Data List");
        attributes.put("00002a37-0000-1000-8000-00805f9b34fb", "Heart Rate Measurement");
        attributes.put("00002a38-0000-1000-8000-00805f9b34fb", "Body Sensor Location");
        attributes.put("00002a39-0000-1000-8000-00805f9b34fb", "Heart Rate Control Point");
        attributes.put("00002a50-0000-1000-8000-00805f9b34fb", "PnP ID");
        attributes.put("00002a53-0000-1000-8000-00805f9b34fb", "RSC Measurement");
        attributes.put("00002a54-0000-1000-8000-00805f9b34fb", "RSC Featur");
        attributes.put("00002a55-0000-1000-8000-00805f9b34fb", "SC Control Point");
        attributes.put("00002a5b-0000-1000-8000-00805f9b34fb", "CSC Measurement");
        attributes.put("00002a5c-0000-1000-8000-00805f9b34fb", "CSC Feature");
        attributes.put("00002a5d-0000-1000-8000-00805f9b34fb", "Sensor Location");

        attributes.put("71261001-3692-ae93-e711-472ba41689c9", "Light Mode");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
