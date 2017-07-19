package com.njackson.test.live;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.live.LiveTracking;
import com.squareup.otto.Bus;

/**
 * Created by njackson on 24/01/15.
 */
public class LiveTrackingTest extends AndroidTestCase {
    @SmallTest
    public void testLiveTrackingGetMsgLiveShort() throws Exception {
        LiveTracking liveTracking = new LiveTracking(LiveTracking.TYPE_JAYPS, new Bus());
        Location location = new Location("JayPS");
        location.setAccuracy(5);
        location.setLatitude(48);
        location.setLongitude(3);
        location.setTime(1420980000000l);
        byte[] msgLiveShort = liveTracking.getMsgLiveShort(location);

        String[] names = liveTracking.getNames();
    }

    @SmallTest
    public void testLiveTrackingParseResponse() throws Exception {
        LiveTracking liveTracking = new LiveTracking(LiveTracking.TYPE_JAYPS, new Bus());

        long time = 1420980000000l;
        Location location = new Location("JayPS");
        location.setAccuracy(5);
        location.setLatitude(48);
        location.setLongitude(3);
        location.setTime(time);

        liveTracking.addPoint(location, location, 200, 98);

        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<message>"
                + "<type>activity_started</type>"
                + "<activity_id>12</activity_id>"
                + "<friends>"
                + "<friend id=\"0\">"
                + "  <id>f1/0</id>"
                + "  <nickname>Friend 1</nickname>"
                + "  <lat>44</lat>"
                + "  <lon>-5</lon>"
                + "  <ts>1421075656</ts>"
                + "</friend>"
                + "<friend id=\"1\">"
                + "  <id>f2</id>"
                + "  <nickname>Friend 2</nickname>"
                + "  <lat>48</lat>"
                + "  <lon>3.00001</lon>"
                + "  <ts>1421175938</ts>"
                + "</friend>"
                + "</friends>"
                + "<points>"
                + "  <point id=\"0\">"
                + "  <lat>49</lat>"
                + "  <lon>7.7931</lon>"
                + "  <ele>0.0</ele>"
                + "  <ts>1421175944</ts>"
                + "  <accuracy>10.0</accuracy>"
                + "</point>"
                + "<point id=\"1\">"
                + "  <lat>49</lat>"
                + "  <lon>7.7932</lon>"
                + "  <ele>0.0</ele>"
                + "  <ts>1421175963</ts>"
                + "  <accuracy>10.0</accuracy>"
                + "</point>"
                + "</points>"
                + "</message>";

        liveTracking.parseResponse("start_activity", response);
        assertEquals(2, liveTracking.numberOfFriends);

        Location location2 = new Location("JayPS");
        location2.setAccuracy(5);
        location2.setLatitude(48.001);
        location2.setLongitude(3.0001);
        location2.setTime(time + 70000);

        liveTracking.addPoint(location, location2, 201, 120);
        liveTracking.parseResponse("update_activity", response);
    }

    @SmallTest
    public void testLiveTrackingGetLogin() throws Exception {
        LiveTracking liveTracking = new LiveTracking(LiveTracking.TYPE_JAYPS, new Bus());
        liveTracking.setLogin("test1");

        assertEquals("test1", liveTracking.getLogin());
    }
    @LargeTest
    // LargeTest => Network access
    public void testLiveTrackingNetworkTest() throws Exception {
        LiveTracking liveTracking = new LiveTracking(LiveTracking.TYPE_JAYPS, new Bus());
        liveTracking.setLogin("test");
        liveTracking.setPassword("test");

        long time = 1420980000000l;
        Location location = new Location("JayPS");
        location.setAccuracy(5);
        location.setLatitude(48);
        location.setLongitude(3);
        location.setTime(time);

        liveTracking.addPoint(location, location, 200, 98);

        Location location2 = new Location("JayPS");
        location2.setAccuracy(5);
        location2.setLatitude(48.001);
        location2.setLongitude(3.0001);
        location2.setTime(time + 70000);

        liveTracking.addPoint(location, location2, 201, 120);
    }
}
