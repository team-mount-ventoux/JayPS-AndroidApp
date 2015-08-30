package com.njackson.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.njackson.R;
import com.njackson.sensor.BLESampleGattAttributes;
import com.njackson.sensor.Ble;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class HRMScanActivity extends ListActivity {

    private static final String TAG = "PB-HRMScanActivity";

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public static boolean debug = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.ble_stop).setVisible(false);
            menu.findItem(R.id.ble_scan).setVisible(true);
            menu.findItem(R.id.ble_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.ble_stop).setVisible(true);
            menu.findItem(R.id.ble_scan).setVisible(false);
            menu.findItem(R.id.ble_refresh).setActionView(R.layout.ble_actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ble_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.ble_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        Intent returnIntent = new Intent();
        returnIntent.putExtra("hrm_name", device.getName());
        returnIntent.putExtra("hrm_address", device.getAddress());
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            //UUID[] uuids = { UUID.fromString(BLESampleGattAttributes.HEART_RATE_SERVICE)};
            //mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = HRMScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.ble_listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.ble_device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.ble_device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.ble_unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    List<UUID> uuids = parseServiceUuids(scanRecord);
                    if (uuids != null) {
                        for (UUID uuid : uuids) {
                            Log.d(TAG, "uuid=" + uuid.toString());
                            if (UUID.fromString(BLESampleGattAttributes.HEART_RATE_SERVICE).equals(uuid)) {
                                Log.d(TAG, "Heart Rate");
                            }
                        }
                    }
                    String msg = "payload = ";
                    for (byte b : scanRecord)
                        msg += String.format("%02x ", b);
                    Log.d(TAG, msg);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
                public List<UUID> parseServiceUuids(final byte[] advertisedData)
                {
                    List<UUID> uuids = new ArrayList<UUID>();

                    if( advertisedData == null )  return uuids;

                    int offset = 0;
                    while(offset < (advertisedData.length - 2))
                    {
                        int len = advertisedData[offset++];
                        if(len == 0)
                            break;

                        int type = advertisedData[offset++];
                        Log.d(TAG, "type="+type);
                        switch(type)
                        {
                            case 0x02: // Partial list of 16-bit UUIDs
                            case 0x03: // Complete list of 16-bit UUIDs
                                while(len > 1)
                                {
                                    int uuid16 = advertisedData[offset++];
                                    uuid16 += (advertisedData[offset++] << 8);
                                    len -= 2;
                                    uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                                }
                                break;
                            case 0x06:// Partial list of 128-bit UUIDs
                            case 0x07:// Complete list of 128-bit UUIDs
                                // Loop through the advertised 128-bit UUID's.
                                while(len >= 16)
                                {
                                    try
                                    {
                                        // Wrap the advertised bits and order them.
                                        ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                                        long mostSignificantBit = buffer.getLong();
                                        long leastSignificantBit = buffer.getLong();
                                        uuids.add(new UUID(leastSignificantBit, mostSignificantBit));
                                    }
                                    catch(IndexOutOfBoundsException e)
                                    {
                                        // Defensive programming.
                                        Log.e(TAG, e.toString());
                                        continue;
                                    }
                                    finally
                                    {
                                        // Move the offset to read the next uuid.
                                        offset += 15;
                                        len -= 16;
                                    }
                                }
                                break;
                            default:
                                offset += (len - 1);
                                break;
                        }
                    }

                    return uuids;
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
