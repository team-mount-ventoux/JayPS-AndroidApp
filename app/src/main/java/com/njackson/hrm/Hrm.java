package com.njackson.hrm;

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
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.HrmServiceCommand.HrmHeartRate;
import com.njackson.utils.time.ITimer;
import com.njackson.utils.time.ITimerHandler;
import com.squareup.otto.Bus;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Hrm implements IHrm, ITimerHandler {

    private final String TAG = "PB-Hrm";

    private final Context _context;
    private Bus _bus;
    private Csc _csc;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress = "";
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static UUID UUID_CSC_MEASUREMENT = UUID.fromString(BLESampleGattAttributes.CSC_MEASUREMENT);

    private boolean debug = true;
    private boolean _hrmStarted = false;
    @Inject ITimer _timer;

    public Hrm(Context context) {
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
                        mBluetoothManager = null;
                        //mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Turning Bluetooth off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth on");

                        if (_hrmStarted) {
                            reconnectLater();
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
    public void start(String hrm_address, Bus bus, IInjectionContainer container) {
        Log.d(TAG, "start");

        container.inject(this);

        _bus = bus;
        mBluetoothDeviceAddress = hrm_address;

        _hrmStarted = true;

        Log.d(TAG, hrm_address);

        // start BLE Heart Rate Monitor
        initialize();
        connect(hrm_address);

        _timer.cancel();

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        _context.registerReceiver(mReceiver, filter);
    }
    @Override
    public void stop() {
        Log.d(TAG, "stop");
        _hrmStarted = false;
        disconnect();

        // Unregister broadcast listeners
        _context.unregisterReceiver(mReceiver);
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        Log.d(TAG, "initialize OK");
        return true;
    }
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    private boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(
                _context,
                // TODO(jay)
                true, /* autoConnect Whether to directly connect to the remote device (false)
                        or to automatically connect as soon as the remote device becomes available (true).*/
                mGattCallback
        );
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private void reconnectLater() {
        if (!_timer.getActive()) {
            _timer.setTimer(10000, this);
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                // TODO(jay) post something?
                //broadcastUpdate(ACTION_GATT_CONNECTED);
                if (debug) Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                boolean discovery = mBluetoothGatt.discoverServices();
                if (debug) Log.i(TAG, "Attempting to start service discovery:" + discovery);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                if (debug) Log.i(TAG, "Disconnected from GATT server.");
                // TODO(jay) post something?
                //broadcastUpdate(ACTION_GATT_DISCONNECTED);

                if (_hrmStarted) {
                    reconnectLater();

                    /*if (debug) Log.i(TAG, "Trying to reconnect.");
                    final boolean result = connect(mBluetoothDeviceAddress);
                    if (debug) Log.d(TAG, "Connect request result=" + result);*/
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (debug) Log.i(TAG, "discovered GATT services.");
                // TODO(jay) post something?
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                displayGattServices();
            } else {
                if (debug) Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (debug)
                Log.d(TAG, "onCharacteristicRead " + characteristic.getUuid().toString() + " status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //if (debug) Log.d(TAG, "onCharacteristicChanged " + characteristic.getUuid().toString());
            broadcastUpdate(characteristic);
        }
    };
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        //if (debug) Log.d(TAG, "broadcastUpdate() uuid="+characteristic.getUuid().toString());

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            //Log.d(TAG, String.format("flag: %d", flag));
            int cumulativeWheelRevolutions = 0;
            int lastWheelEventTime = 0;
            int cumulativeCrankRevolutions = 0;
            int lastCrankEventTime = 0;

           // if ((flag & 0x01) != 0) {
                // Wheel Revolution Data Present
                cumulativeWheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32 , 1);
                lastWheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16 , 5);

            //}
            //if ((flag & 0x02) != 0) {
                //Crank Revolution Data Presen
                cumulativeCrankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
                lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
            //}

            _csc.onNewValues(cumulativeWheelRevolutions, lastWheelEventTime, cumulativeCrankRevolutions, lastCrankEventTime);

            _bus.post(new HrmHeartRate((int) _csc.getCrankRpm()));
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
                if (debug) Log.d(TAG, "broadcastUpdate():" + new String(data) + "\n" + stringBuilder.toString());
            }
        }
    }
    private void displayGattServices() {
        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                if (UUID_CSC_MEASUREMENT.equals(gattCharacteristic.getUuid())) {
                    //if (MainActivity.debug) Log.d(TAG, "UUID_CSC_MEASUREMENT!");
                    int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            setCharacteristicNotification(mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        //if (MainActivity.debug) Log.d(TAG, "readCharacteristic: " + gattCharacteristic.getUuid().toString());
                        readCharacteristic(gattCharacteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = gattCharacteristic;
                        //if (MainActivity.debug) Log.d(TAG, "setCharacteristicNotification: " + gattCharacteristic.getUuid().toString());
                        setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //if (MainActivity.debug) Log.d(TAG, "readCharacteristic "+characteristic.getUuid().toString());
        mBluetoothGatt.readCharacteristic(characteristic);
    }
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //if (debug) Log.w(TAG, "setCharacteristicNotification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BLESampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    @Override
    public void handleTimeout() {
        Log.d(TAG,"handleTimeout");
        initialize();
        final boolean result = connect(mBluetoothDeviceAddress);
    }
}
