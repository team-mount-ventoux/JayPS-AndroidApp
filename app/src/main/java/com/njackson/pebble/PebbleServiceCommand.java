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

    private static final String TAG = "PB-PebbleService";

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        if (newLocation.getTime() > 0) {
            if (!_sharedPreferences.getString("CANVAS_MODE", "disable").equals("canvas_only")) {
                sendLocationToPebble(newLocation);
            }

            if (!_sharedPreferences.getString("CANVAS_MODE", "disable").equals("disable")) {
                sendLocationToCanvas(newLocation);
            }
        }
    }

    @Subscribe
    public void onGPSServiceState(GPSStatus event) {
        if(event.getStatus().compareTo(BaseStatus.Status.STARTED) == 0) {
            if (!_sharedPreferences.getString("CANVAS_MODE", "disable").equals("canvas_only")) {
                _messageManager.showWatchFace();
            }
        } else if (event.getStatus().compareTo(BaseStatus.Status.STOPPED) == 0) {
            notifyPebbleGPSStopped();
        }
    }

    @Subscribe
    public void onNewMessageEvent(NewMessage message) {
        _messageManager.showSimpleNotificationOnWatch("Pebble Bike", message.getMessage());
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
    }

    private void notifyPebbleGPSStopped() {
        PebbleDictionary dictionary = new PebbleDictionary();
        dictionary.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
        sendDataToPebble(dictionary);
    }

    private void sendLiveMessage(LiveMessage message) {
        PebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        sendDataToPebble(dic);
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
                true, // TODO(nic)
                _sharedPreferences.getBoolean("PREF_DEBUG", false),
                _sharedPreferences.getBoolean("LIVE_TRACKING", false),
                Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000")),
                newLocation.getHeartRate()
        );
        sendDataToPebble(dictionary);
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