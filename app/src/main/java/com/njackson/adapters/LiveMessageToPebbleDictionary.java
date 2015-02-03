package com.njackson.adapters;

import android.util.Log;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.events.LiveServiceCommand.LiveMessage;

/**
 * Created by njackson on 24/01/15.
 */
public class LiveMessageToPebbleDictionary extends PebbleDictionary {

    private static final String TAG = "PB-LiveMessageToPebbleDictionary";

    private int _numberOfFriendsSentToPebble = 0;
    public boolean _forceSend = true;

    public boolean getForceSend() { return _forceSend; }

    public LiveMessageToPebbleDictionary(LiveMessage message) {

        _forceSend = false;

        String sending = "";
        if (message.getLive().length > 0) {
            if (_numberOfFriendsSentToPebble != message.getLive()[0] || (5 * Math.random() <= 1)) {
                _numberOfFriendsSentToPebble = message.getLive()[0];
                if (message.getName0() != null) {
                    this.addString(Constants.MSG_LIVE_NAME0, message.getName0());
                }
                if (message.getName1() != null) {
                    this.addString(Constants.MSG_LIVE_NAME1, message.getName1());
                }
                if (message.getName2() != null) {
                    this.addString(Constants.MSG_LIVE_NAME2, message.getName2());
                }
                if (message.getName3() != null) {
                    this.addString(Constants.MSG_LIVE_NAME3, message.getName3());
                }
                if (message.getName4() != null) {
                    this.addString(Constants.MSG_LIVE_NAME4, message.getName4());
                }
                sending += " MSG_LIVE_NAMEx" + message.getLive()[0];
                _forceSend = true;
            }
            this.addBytes(Constants.MSG_LIVE_SHORT, message.getLive());
            for (int i = 0; i < message.getLive().length; i++) {
                sending += " msgLiveShort[" + i + "]: " + ((256 + message.getLive()[i]) % 256);
            }
            //Log.d(TAG, sending);
        }
    }

}
