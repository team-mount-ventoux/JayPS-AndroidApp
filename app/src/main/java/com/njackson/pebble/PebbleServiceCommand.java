package com.njackson.pebble;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.adapters.LiveMessageToPebbleDictionary;
import com.njackson.adapters.NewLocationToCanvasPluginGPSData;
import com.njackson.adapters.NewLocationToPebbleDictionary;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.PebbleServiceCommand.NewMessage;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.LiveServiceCommand.LiveMessage;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.Navigator;
import com.njackson.pebble.canvas.GPSData;
import com.njackson.pebble.canvas.ICanvasWrapper;
import com.njackson.service.IServiceCommand;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;


public class PebbleServiceCommand implements IServiceCommand {

    @Inject @ForApplication Context _applicationContext;
    @Inject IMessageManager _messageManager;
    @Inject ICanvasWrapper _canvasWrapper;
    @Inject SharedPreferences _sharedPreferences;
    @Inject Bus _bus;
    @Inject Navigator _navigator;

    private static final String TAG = "PB-PebbleServiceCommand";
    private BaseStatus.Status _currentStatus = BaseStatus.Status.NOT_INITIALIZED;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        if (!_sharedPreferences.getString("CANVAS_MODE", "disable").equals("canvas_only")) {
            sendLocationToPebble(newLocation);
        }

        if (!_sharedPreferences.getString("CANVAS_MODE", "disable").equals("disable")) {
            sendLocationToCanvas(newLocation);
        }
    }

    @Subscribe
    public void onGPSServiceState(GPSStatus event) {
        if(event.getStatus() == BaseStatus.Status.STARTED) {
            if (!_sharedPreferences.getString("CANVAS_MODE", "disable").equals("canvas_only")) {
                _messageManager.showWatchFace();
            }
            notifyPebbleGPSStarted();
        } else if (event.getStatus() == BaseStatus.Status.STOPPED) {
            notifyPebbleGPSStopped();
        } else if (event.getStatus() == BaseStatus.Status.DISABLED) {
            notifyPebbleGPSDisable();
        }
    }

    @Subscribe
    public void onNewMessageEvent(NewMessage message) {
        _messageManager.showSimpleNotificationOnWatch("JayPS", message.getMessage());
    }

    @Subscribe
    public void onLiveMessage(LiveMessage msg) {
        Log.d(TAG, "onLiveMessage");
        sendLiveMessage(msg);
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
        _currentStatus = BaseStatus.Status.INITIALIZED;
    }

    @Override
    public void dispose() {
        _bus.unregister(this);
    }

    @Override
    public BaseStatus.Status getStatus() {
        return _currentStatus;
    }

    private void notifyPebbleGPSStarted() {
        //Log.i(TAG, "notifyPebbleGPSStarted");
        PebbleDictionary dictionary = new PebbleDictionary();
        dictionary.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
        sendDataToPebble(dictionary);
    }
    private void notifyPebbleGPSStopped() {
        //Log.i(TAG, "notifyPebbleGPSStopped");
        PebbleDictionary dictionary = new PebbleDictionary();
        dictionary.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
        sendDataToPebble(dictionary);
    }

    private void notifyPebbleGPSDisable() {
        _messageManager.showSimpleNotificationOnWatch("JayPS", "GPS is disabled on your phone. Please enable it.");

    }

    private void sendLiveMessage(LiveMessage message) {
        LiveMessageToPebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        sendDataToPebble(dic, dic.getForceSend());
    }

    private void sendLocationToCanvas(NewLocation newLocation) {
        GPSData data = new NewLocationToCanvasPluginGPSData(
                newLocation,
                _sharedPreferences.getBoolean("CANVAS_DISPLAY_UNITS", true)
        );
        _canvasWrapper.set_gpsdata_details(data, _applicationContext);
    }

    private void sendLocationToPebble(NewLocation newLocation) {
        PebbleDictionary dictionary = new NewLocationToPebbleDictionary(
                newLocation,
                _navigator,
                true, // forced to true, we're receiving location (can a location arrive after stop event?)
                _sharedPreferences.getBoolean("PREF_DEBUG", false),
                _sharedPreferences.getBoolean("LIVE_TRACKING", false),
                Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", String.valueOf(Constants.REFRESH_INTERVAL_DEFAULT))),
                _sharedPreferences.getInt("WATCHFACE_VERSION", 0),
                _sharedPreferences.getBoolean("NAV_NOTIFICATION", false)
        );
        sendDataToPebbleIfPossible(dictionary);
    }

    private void sendDataToPebble(PebbleDictionary data) {
        _messageManager.offer(data);
    }

    private void sendDataToPebbleIfPossible(PebbleDictionary data) {
        _messageManager.offerIfLow(data, 5);
    }

    private void sendDataToPebble(PebbleDictionary data, boolean forceSend) {
        if (forceSend) {
            sendDataToPebble(data);
        } else {
            sendDataToPebbleIfPossible(data);
        }
    }
}