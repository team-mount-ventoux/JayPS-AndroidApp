package com.njackson.adapters;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.events.LiveServiceCommand.LiveMessage;

/**
 * Created by njackson on 24/01/15.
 */
public class LiveMessageToPebbleDictionary extends PebbleDictionary {

    public LiveMessageToPebbleDictionary(LiveMessage message) {
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
    }

}
