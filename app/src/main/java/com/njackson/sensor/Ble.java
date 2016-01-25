package com.njackson.sensor;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.njackson.application.IInjectionContainer;
import com.njackson.events.BleServiceCommand.BleSensorData;
import com.njackson.utils.time.ITimer;
import com.njackson.utils.time.ITimerHandler;
import com.squareup.otto.Bus;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Ble implements IBle, ITimerHandler {

    private final String TAG = "PB-Ble";

    private final Context _context;
    private Bus _bus;
    private Csc _csc;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(BLESampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_CSC_MEASUREMENT = UUID.fromString(BLESampleGattAttributes.CSC_MEASUREMENT);
    public final static UUID UUID_RSC_MEASUREMENT = UUID.fromString(BLESampleGattAttributes.RSC_MEASUREMENT);
    public final static UUID UUID_BATTERY_LEVEL = UUID.fromString(BLESampleGattAttributes.BATTERY_LEVEL);
    public final static UUID UUID_TEMPERATURE_MEASUREMENT = UUID.fromString(BLESampleGattAttributes.TEMPERATURE_MEASUREMENT);

    private final static int TIMEOUT_CONNECTGATT = 5 * 60 * 1000; // in ms

    private boolean debug = true;
    private boolean _bleStarted = false;
    @Inject ITimer _timer;

    private Queue<BluetoothDevice> connectionQueue = new LinkedList<BluetoothDevice>();
    private Thread connectionThread;
    private Queue<BluetoothGatt> serviceDiscoveryQueue = new LinkedList<BluetoothGatt>();
    private Thread serviceDiscoveryThread;
    private ConcurrentHashMap<String, BluetoothGatt> mGatts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, BluetoothGatt> mGattsConnectionPending = new ConcurrentHashMap<>();
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> readCharacteristicQueue = new LinkedList<BluetoothGattCharacteristic>();
    private boolean allwrites = false;
    private int _nbReconnect = 0;
    private String _ble_address1 = "";
    private String _ble_address2 = "";
    private String _ble_address3 = "";

    public Ble(Context context) {
        _context = context;
        _csc = new Csc();
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Turning Bluetooth off...");
                        mBluetoothManager = null;
                        disconnectAllDevices();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth on");

                        if (_bleStarted) {
                            initialize();
                        }

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Turning Bluetooth on...");
                        break;
                }
            }
        }
    };
    @Override
    public void start(String ble_address1, String ble_address2, String ble_address3, Bus bus, IInjectionContainer container) {
        Log.d(TAG, "start");

        container.inject(this);
        _bus = bus;
        _bleStarted = true;

        // for later reconnections
        _ble_address1 = ble_address1;
        _ble_address2 = ble_address2;
        _ble_address3 = ble_address3;

//        String BLE_JAY_HRM1 = "1C:BA:8C:1F:58:1D";
//        String BLE_JAY_HRM2 = "E0:C7:9D:69:1E:57";
//        String BLE_JAY_CSC  = "EB:18:F4:AA:92:4E";
//        _ble_address1 = BLE_JAY_HRM1;_ble_address2 = BLE_JAY_HRM2;

        Log.d(TAG, "_ble_address1=" + _ble_address1 + " _ble_address2 = " + _ble_address2 + " _ble_address3 = " + _ble_address3);

        initialize();

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        _context.registerReceiver(mReceiver, filter);
    }
    @Override
    public void stop() {
        Log.d(TAG, "stop");
        _bleStarted = false;
        disconnectAllDevices();

        // Unregister broadcast listeners
        _context.unregisterReceiver(mReceiver);
    }


    public void initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return;
        }

        Log.d(TAG, "initialize OK");

        if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
            Log.e(TAG, "Bluetooth is not ON");
            // new attempt to connect will be done when receiving BluetoothAdapter.ACTION_STATE_CHANGED
            return;
        } else {
            Log.d(TAG, "initConnections " + _ble_address1 + " " + _ble_address2 + " " + _ble_address3);

            if (mBluetoothAdapter == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            if (_ble_address1.equals(_ble_address2)) {
                // do not connect twice to the same device
                _ble_address2 = "";
            }
            if (_ble_address3.equals(_ble_address2) || _ble_address3.equals(_ble_address1)) {
                // do not connect twice to the same device
                _ble_address3 = "";
            }
            BluetoothDevice device1 = null;
            BluetoothDevice device2 = null;
            BluetoothDevice device3 = null;
            if (!_ble_address1.equals("")) {
                device1 = mBluetoothAdapter.getRemoteDevice(_ble_address1);
                if (device1 == null) {
                    Log.w(TAG, "Device1 not found. Unable to connect.");
                    return;
                }
                connectionQueue.add(device1);
            }
            if (!_ble_address2.equals("")) {
                device2 = mBluetoothAdapter.getRemoteDevice(_ble_address2);
                if (device2 == null) {
                    Log.w(TAG, "Device2 not found. Unable to connect.");
                    return;
                }
                connectionQueue.add(device2);
            }
            if (!_ble_address3.equals("")) {
                device3 = mBluetoothAdapter.getRemoteDevice(_ble_address3);
                if (device3 == null) {
                    Log.w(TAG, "Device3 not found. Unable to connect.");
                    return;
                }
                connectionQueue.add(device3);
            }
            if (device1 != null || device2 != null || device3 != null) {
                if (connectionThread == null) {
                    connectionThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            connectionLoop();
                            //                    connectionThread.interrupt();
                            //                    connectionThread = null;
                        }
                    });

                    connectionThread.start();
                }
            }
        }
    }

    private void connectionLoop() {
        while(connectionThread != null) {
            while (!connectionQueue.isEmpty()) {
                BluetoothDevice device = connectionQueue.poll();
                Log.d(TAG, "connectionLoop next device " + device.getAddress().toString());
                // Official doc: Cancel the current device discovery process.
                // An application should always call cancel discovery even if it did not directly request a discovery, just to be sure.
                mBluetoothAdapter.cancelDiscovery();

                // force new timer (to cancel connectGatt() if its callback is not called in TIMEOUT_CONNECTGATT ms)
                _timer.cancel();
                _timer.setTimer(TIMEOUT_CONNECTGATT, this);

                BluetoothGatt gatt = device.connectGatt(_context, false, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        Log.d(TAG, display(gatt) + " onConnectionStateChange status=" + status + " newState=" + newState);

                        if (status == 133) {
                            // http://stackoverflow.com/questions/21021429/bluetoothlowenergy-range-issue-android
                            Log.d(TAG, display(gatt) + " status=133, not in range?");
                        }

                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            _nbReconnect = 0;
                            // TODO(jay) post something?
                            //broadcastUpdate(ACTION_GATT_CONNECTED);
                            if (debug) Log.i(TAG, display(gatt) + " Connected to GATT server.");

                            mGattsConnectionPending.remove(gatt.getDevice().getAddress());
                            Log.d(TAG, "after remove, mGattsConnectionPending.size:" + mGattsConnectionPending.size());

                            // Attempts to discover services after successful connection.
                            //boolean discovery = mBluetoothGatt.discoverServices();
                            serviceDiscoveryQueue.add(gatt);

                            Log.d(TAG, "connectionQueue.size=" + connectionQueue.size());
                            //if (connectionQueue.isEmpty()) {
                            initServiceDiscovery();
                            //}


                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if (debug) Log.i(TAG, display(gatt) + " Disconnected from GATT server.");
                            // TODO(jay) post something?
                            //broadcastUpdate(ACTION_GATT_DISCONNECTED);

                            gatt.close();
                            mGatts.remove(gatt.getDevice().getAddress());
                            //Log.d(TAG, "after remove, mGatts.size:" + mGatts.size());
                            if (_bleStarted) {
                                reconnectLater(gatt);
                            }
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.d(TAG, "onServicesDiscovered");
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (debug) Log.i(TAG, display(gatt) + " discovered GATT services.");
                            // TODO(jay) post something?
                            //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                            displayGattServices(gatt);
                        } else {
                            if (debug) Log.w(TAG, display(gatt) + " onServicesDiscovered received: " + status);
                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt,
                                                     BluetoothGattCharacteristic characteristic,
                                                     int status) {
                        readCharacteristicQueue.remove();

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            String msg = decodeCharacteristic(gatt, characteristic);
                            if (debug) Log.d(TAG, display(gatt, characteristic) + " onCharacteristicRead status=" + status + msg);
                        } else {
                            Log.d(TAG, display(gatt, characteristic) + " onCharacteristicRead error: " + status);
                        }
                        if (readCharacteristicQueue.size() > 0) {
                            gatt.readCharacteristic(readCharacteristicQueue.element());
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt,
                                                        BluetoothGattCharacteristic characteristic) {
                        String msg = decodeCharacteristic(gatt, characteristic);
                        if (debug) Log.d(TAG, display(gatt) + " onCharacteristicChanged" + display(characteristic) + " " + msg);
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        Log.d(TAG, display(gatt) + " onMtuChanged mtu=" + mtu + " status=" + status);
                    }

                    @Override
                    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                        Log.d(TAG, display(gatt) + " onReadRemoteRssi rssi=" + rssi + " status=" + status);
                    }

                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, display(gatt) + " Callback: Wrote GATT Descriptor successfully.");
                        } else {
                            Log.d(TAG, display(gatt) + " Callback: Error writing GATT Descriptor: " + status);
                        }
                        descriptorWriteQueue.remove();  //pop the item that we just finishing writing
                        //if there is more to write, do it!
                        if (descriptorWriteQueue.size() > 0) {
                            Log.d(TAG, display(gatt) + " write next descriptor");
                            gatt.writeDescriptor(descriptorWriteQueue.element());
                        } else if (readCharacteristicQueue.size() > 0) {
                            Log.d(TAG, display(gatt) + " no more descriptor, next characteristic");
                            gatt.readCharacteristic(readCharacteristicQueue.element());
                        }
                    }
                });

                mGatts.put(gatt.getDevice().getAddress(), gatt);
                //Log.d(TAG, "put " + display(gatt) + " into mGatts, size:" + mGatts.size());
                mGattsConnectionPending.put(gatt.getDevice().getAddress(), gatt);
                ///Log.d(TAG, "put " + display(gatt) + " into mGattsConnectionPending, size:" + mGattsConnectionPending.size());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void disconnectAllDevices() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "disconnectAllDevices BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "disconnectAllDevices mGatts.size:" + mGatts.size());
        Iterator<Map.Entry<String, BluetoothGatt>> iterator = mGatts.entrySet().iterator();
        while (iterator.hasNext()) {
            BluetoothGatt gatt = iterator.next().getValue();
            Log.d(TAG, "disconnect" + display(gatt));
            if (gatt != null) {
                gatt.disconnect();
                gatt.close();
                //gatt = null;
            }
        }
        mGatts.clear();
        if (connectionThread != null) {
            connectionThread.interrupt();
            connectionThread = null;
        }
    }

    private void reconnectLater(BluetoothGatt gatt) {
        // Note: reconnectLater can be called just after STATE_TURNING_OFF
        if (mBluetoothAdapter == null || mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
            return;
        }

        _nbReconnect++;
        try {
            int sleep;
            if (_nbReconnect < 5) {
                sleep = 5;
            } else if(_nbReconnect <= 20) {
                sleep = 15;
            } else {
                sleep = 60;
            }
            Log.w(TAG, display(gatt) + " reconnectLater, _nbReconnect: " + _nbReconnect + " wait " + sleep + "s");
            Thread.sleep(1000 * sleep);
        } catch (InterruptedException e) {
        }
        if (_bleStarted) {

            if (mGattsConnectionPending.containsKey(gatt.getDevice().getAddress())) {
                Log.w(TAG, display(gatt) + " reconnectLater already in mGattsConnectionPending, skip it");
            } else {
                Log.w(TAG, display(gatt) + " reconnectLater connectionQueue.add");
                connectionQueue.add(gatt.getDevice());
            }
        }
    }

    private void initServiceDiscovery() {
        Log.d(TAG, "initServiceDiscovery");
        if(serviceDiscoveryThread == null){
            serviceDiscoveryThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    serviceDiscovery();

                    //Log.d(TAG, "before serviceDiscoveryThread.interrupt");
                    serviceDiscoveryThread.interrupt();
                    serviceDiscoveryThread = null;
                }
            });

            serviceDiscoveryThread.start();
        }
    }

    private void serviceDiscovery() {
        Log.d(TAG, "serviceDiscovery start");
        while(!serviceDiscoveryQueue.isEmpty()){
            BluetoothGatt gatt = serviceDiscoveryQueue.poll();
            Log.d(TAG, "serviceDiscovery next device " + display(gatt));
            gatt.discoverServices();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e){}
        }
        Log.d(TAG, "serviceDiscovery end");
    }

    private String decodeCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        String res = "";
        //if (debug) Log.d(TAG, "decodeCharacteristic() "+display(gatt, characteristic));

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.d(TAG, String.format("flags: %d|%s", flags, Integer.toBinaryString(flags)));
            int sensorContactStatus = (flags & 0x06) >> 1;
            // 0,1	Sensor Contact feature is not supported in the current connection
            // 2 	Sensor Contact feature is supported, but contact is not detected
            // 3 	Sensor Contact feature is supported and contact is detected
            Log.d(TAG, "sensorContactStatus=" + sensorContactStatus);
            /*byte[] values = characteristic.getValue();
            String tmp = "";
            for(int i=0; i<values.length; i++) {
                tmp += String.format("|%d(%02X)", values[i], values[i]);
            }
            Log.d(TAG, "characteristic HRM=" + tmp);*/
            int offset = 1;
            int format = -1;
            if ((flags & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                offset += 2;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                offset += 1;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            res = String.format("Received heart rate: %d", heartRate);

            if ((flags & (1 << 3)) != 0) {
                // calories present
                int energy = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;
                Log.d(TAG, "Received energy: " + energy);
            }
            if ( (flags & (1 << 4)) != 0) {
                // RR interval.
                int rrCount = ((characteristic.getValue()).length - offset) / 2;
                int[] rrIntervals = new int[rrCount];
                String tmp = "";
                for (int i = 0; i < rrCount; i++){
                    rrIntervals[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;
                    tmp += " " + rrIntervals[i];
                }
                Log.d(TAG, "Received rrIntervals: " + tmp);
            }

            BleSensorData sensorData = new BleSensorData(gatt.getDevice().getAddress());
            sensorData.setHeartRate(heartRate);
            //sensorData.setCyclingWheelRpm(3 * heartRate); // fake values to debug csc
            _bus.post(sensorData);
        } else if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.d(TAG, String.format("flags: %d|%s", flags, Integer.toBinaryString(flags)));
            /*byte[] values = characteristic.getValue();
            String tmp = "";
            for(int i=0; i<values.length; i++) {
                tmp += String.format("|%d(%02X)", values[i], values[i]);
            }
            Log.d(TAG, "characteristic CSC=" + tmp);*/
            boolean wheelRevolutionDataPresent = false;
            boolean crankRevolutionDataPresent = false;
            int cumulativeWheelRevolutions = 0;
            int lastWheelEventTime = 0;
            int cumulativeCrankRevolutions = 0;
            int lastCrankEventTime = 0;
            int offset = 0;

            if ((flags & 0x01) != 0) {
                cumulativeWheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32 , 1);
                lastWheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16 , 5);
                wheelRevolutionDataPresent = true;
                offset += 6;
                Log.d(TAG, "Received wheelRevolutionData");
            }
            if ((flags & 0x02) != 0) {
                cumulativeCrankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1+offset);
                lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3+offset);
                crankRevolutionDataPresent = true;
                Log.d(TAG, "Received crankRevolutionData");
            }
            boolean needToPostData = _csc.onNewValues(cumulativeWheelRevolutions, lastWheelEventTime, cumulativeCrankRevolutions, lastCrankEventTime);

            res = String.format("Received cadence: %d, wheelRpm: %d %s", (int) _csc.getCrankRpm(), (int) _csc.getWheelRpm(), needToPostData ? "[NEW]" : "");

            if (needToPostData && crankRevolutionDataPresent) {
                BleSensorData sensorData = new BleSensorData(gatt.getDevice().getAddress());
                sensorData.setCyclingCadence((int) _csc.getCrankRpm());
                _bus.post(sensorData);
            }
            if (needToPostData && wheelRevolutionDataPresent) {
                BleSensorData sensorData = new BleSensorData(gatt.getDevice().getAddress());
                sensorData.setCyclingWheelRpm(_csc.getWheelRpm());
                _bus.post(sensorData);
            }
        } else if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            final int battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            res = String.format("Received battery: %d", battery);
        } else if (UUID_TEMPERATURE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.d(TAG, String.format("flags: %d|%s", flags, Integer.toBinaryString(flags)));
            /*byte[] values = characteristic.getValue();
            String tmp = "";
            for(int i=0; i<values.length; i++) {
                tmp += String.format("|%d(%02X)", values[i], values[i]);
            }
            Log.d(TAG, "characteristic Temperature=" + tmp);*/
            int offset = 0;
            String units = "";
            if ((flags & 0x00) == 0) {
                units = "Celsius";
                offset = 1;
            } else {
                units = "Fahrenheit";
                offset = 2;
            }
            float temperature = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, offset);
            res = String.format("Received temperature: %f %s", temperature, units);
            if (offset == 2) {
                // force conversion to celsius
                temperature = ((temperature - 32) * 5) / 9;
            }
            BleSensorData sensorData = new BleSensorData(gatt.getDevice().getAddress());
            sensorData.setTemperature(temperature);
            _bus.post(sensorData);
        } else if (UUID_RSC_MEASUREMENT.equals(characteristic.getUuid())) {
            int speed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
            int cadence = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);

            res = String.format("Received running speeed: %d m/s, running cadence: %d", speed, cadence);
            BleSensorData sensorData = new BleSensorData(gatt.getDevice().getAddress());
            sensorData.setRunningCadence((int) cadence);
            _bus.post(sensorData);

        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }
                //intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                // TODO(jay) post something?
                res = "Received data:" + display(characteristic) + "=>" + stringBuilder.toString();
            }
        }
        return res;
    }

    private void displayGattServices(BluetoothGatt gatt) {
        Log.d(TAG, "displayGattServices");
        List<BluetoothGattService> gattServices = gatt.getServices();
        allwrites = false;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            if (debug) Log.i(TAG, display(gatt) + " displayGattServices gattService: " + display(gattService));

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                int charaProp = gattCharacteristic.getProperties();
                if (debug) Log.i(TAG, display(gatt) + " displayGattServices characteristic: " +  display(gattCharacteristic) + " charaProp=" + charaProp);
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                    if (gattCharacteristic.getUuid().toString().equals("00002a00-0000-1000-8000-00805f9b34fb") // device name
//                            || gattCharacteristic.getUuid().toString().equals("00002a38-0000-1000-8000-00805f9b34fb") // Body Sensor Location
//                     ) {
                        //readCharacteristic(gattCharacteristic);
                    //}
                }
                if (
                        UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())
                        || UUID_CSC_MEASUREMENT.equals(gattCharacteristic.getUuid())
                        || UUID_RSC_MEASUREMENT.equals(gattCharacteristic.getUuid())
                        || UUID_BATTERY_LEVEL.equals(gattCharacteristic.getUuid())
                        || UUID_TEMPERATURE_MEASUREMENT.equals(gattCharacteristic.getUuid())

                ) {
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        setCharacteristicNotification(gatt, gattCharacteristic, true);
                    }
                }
            }
        }
        Log.d(TAG, "descriptorWriteQueue.size=" + descriptorWriteQueue.size());
        if (descriptorWriteQueue.size() > 0) {
            gatt.writeDescriptor(descriptorWriteQueue.element());
        }
        allwrites = true;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "readCharacteristic not supported" + display(characteristic));
//        Log.d(TAG, "readCharacteristic " + display(characteristic));
//        if (mBluetoothAdapter == null /*|| mBluetoothGatt == null*/) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        //if (MainActivity.debug) Log.d(TAG, "readCharacteristic "+characteristic.getUuid().toString());
//        //mBluetoothGatt.readCharacteristic(characteristic);
//
//        //put the characteristic into the read queue
//        readCharacteristicQueue.add(characteristic);
//        //if there is only 1 item in the queue, then read it.  If more than 1, we handle asynchronously in the callback above
//        //GIVE PRECEDENCE to descriptor writes.  They must all finish first.
//        if((readCharacteristicQueue.size() == 1) && (descriptorWriteQueue.size() == 0) && allwrites)
//            mBluetoothGatt.readCharacteristic(characteristic);
    }
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    private void setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        Log.d(TAG, display(gatt) + " setCharacteristicNotification " + display(characteristic));
        if (mBluetoothAdapter == null || gatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //if (debug) Log.w(TAG, "setCharacteristicNotification");
        gatt.setCharacteristicNotification(characteristic, enabled);
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        // This is specific to Heart Rate Measurement.
        /*if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())
            || UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())
            || UUID_RSC_MEASUREMENT.equals(characteristic.getUuid())
        ) {*/
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(BLESampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            writeGattDescriptor(gatt, descriptor);
        /*} else {
            if (debug) Log.i(TAG, "unused characteristics2:" + display(gatt, characteristic));
        }*/
    }

    public void writeGattDescriptor(BluetoothGatt gatt, BluetoothGattDescriptor d){
        //put the descriptor into the write queue
        descriptorWriteQueue.add(d);
        //if there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
//        if(descriptorWriteQueue.size() == 1) {
//            gatt.writeDescriptor(d);
//        }
    }

    public String display(BluetoothGattService gattService) {
        return " s:"+gattService.getUuid().toString().substring(4, 8) + " " + BLESampleGattAttributes.lookup(gattService.getUuid().toString(), "UNK");
    }
    public String display(BluetoothGattCharacteristic characteristic) {
        return " c:"+characteristic.getUuid().toString().substring(4, 8) + " " + BLESampleGattAttributes.lookup(characteristic.getUuid().toString(), "UNK");
    }
    public String display(BluetoothGatt gatt) {
        return " @:"+gatt.getDevice().getAddress().toString().substring(0, 5);
    }
    public String display(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return display(gatt) + display(characteristic);
    }


    @Override
    public void handleTimeout() {
        // timeout TIMEOUT_CONNECTGATT ms after last attempt to connectGatt
        // force cancel of pending connections

        if (mGattsConnectionPending.size() == 0) {
            // no pending connections, nothing to do
            return;
        }
        Log.d(TAG, "handleTimeout mGattsConnectionPending.size:" + mGattsConnectionPending.size());
        Iterator<Map.Entry<String, BluetoothGatt>> iterator = mGattsConnectionPending.entrySet().iterator();
        while (iterator.hasNext()) {
            BluetoothGatt gatt = iterator.next().getValue();
            Log.d(TAG, "handleTimeout close pending connection to " + display(gatt));
            gatt.close();
            mGattsConnectionPending.remove(gatt.getDevice().getAddress());
            reconnectLater(gatt);
        }
    }
}
