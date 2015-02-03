package com.njackson.test.adapters;

import android.test.AndroidTestCase;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.adapters.LiveMessageToPebbleDictionary;
import com.njackson.events.LiveServiceCommand.LiveMessage;
import com.njackson.live.LiveTracking;

/**
 * Created by njackson on 24/01/15.
 */
public class LiveMessageToPebbleDictionaryTest extends AndroidTestCase {

    public void testSetsName0() {
        LiveMessage message = new LiveMessage();
        byte[] data = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
        data[0] = 5; // numberOfFriends
        message.setLive(data);
        message.setName0("zero");
        PebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        assertEquals("zero", dic.getString(Constants.MSG_LIVE_NAME0));
    }

    public void testSetsName1() {
        LiveMessage message = new LiveMessage();
        byte[] data = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
        data[0] = 5; // numberOfFriends
        message.setLive(data);
        message.setName1("one");
        PebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        assertEquals("one", dic.getString(Constants.MSG_LIVE_NAME1));
    }

    public void testSetsName2() {
        LiveMessage message = new LiveMessage();
        byte[] data = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
        data[0] = 5; // numberOfFriends
        message.setLive(data);
        message.setName2("two");
        PebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        assertEquals("two", dic.getString(Constants.MSG_LIVE_NAME2));
    }

    public void testSetsName3() {
        LiveMessage message = new LiveMessage();
        byte[] data = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
        data[0] = 5; // numberOfFriends
        message.setLive(data);
        message.setName3("three");
        PebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        assertEquals("three", dic.getString(Constants.MSG_LIVE_NAME3));
    }

    public void testSetsName4() {
        LiveMessage message = new LiveMessage();
        byte[] data = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
        data[0] = 5; // numberOfFriends
        message.setLive(data);
        message.setName4("four");
        PebbleDictionary dic = new LiveMessageToPebbleDictionary(message);

        assertEquals("four", dic.getString(Constants.MSG_LIVE_NAME4));
    }


}
