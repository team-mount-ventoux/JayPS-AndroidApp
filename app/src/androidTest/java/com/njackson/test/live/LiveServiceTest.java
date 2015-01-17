package com.njackson.test.live;

import android.content.Intent;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.live.LiveService;
import com.njackson.live.LiveTracking;
import com.njackson.live.LiveTrackingFriend;
import com.njackson.test.application.TestApplication;
import com.njackson.pebble.IMessageManager;

import com.squareup.otto.Bus;

import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import android.location.Location;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


import java.io.ByteArrayInputStream;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

public class LiveServiceTest extends ServiceTestCase<LiveService>{

    private static final String TAG = "PB-LiveServiceTest";

    @Inject Bus _bus;
    @Inject IMessageManager _mockMessageManager;
    @Inject SharedPreferences _mockPreferences;

    private LiveService _service;
    private TestApplication _app;
    //private CurrentState _pebbleStatusEvent;
    //private CountDownLatch _stateLatch;

    public LiveServiceTest() {
        super(LiveService.class);
    }

    @Module(
            includes = PebbleBikeModule.class,
            injects = LiveServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        public IMessageManager providesMessageManager() {
            return mock(IMessageManager.class);
        }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        _service = new LiveService();

        System.setProperty("dexmaker.dexcache", getSystemContext().getCacheDir().getPath());

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);
        _bus.register(this);

        setApplication(_app);

        setupMocks();

        //_stateLatch = new CountDownLatch(1);
    }

    @Override
    public void tearDown() throws Exception {
        reset(_mockMessageManager);
        super.tearDown();
    }
    private void setupMocks() {
        when(_mockPreferences.getString("REFRESH_INTERVAL", "1000")).thenReturn("1000");
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(true);
        when(_mockPreferences.getBoolean("PREF_DEBUG", false)).thenReturn(true);
        when(_mockPreferences.getString("LIVE_TRACKING_LOGIN", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_PASSWORD", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_URL", "")).thenReturn("");
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_LOGIN", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_URL", "")).thenReturn("");
    }

    private void startService() throws InterruptedException {
        Intent startIntent = new Intent(getSystemContext(), LiveService.class);
        startService(startIntent);
        _service = getService();

        //_stateLatch.await(2000,  TimeUnit.MILLISECONDS);
    }
    @SmallTest
    public void testEventOnNewLocationEvent() throws Exception {

        startService();

        ArgumentCaptor<PebbleDictionary> captor = new ArgumentCaptor<PebbleDictionary>();

        long time = 1420980000;

        NewLocation event = new NewLocation();
        event.setLatitude(45.0);
        event.setLongitude(8.0);
        event.setTime(time);
        event.setHeartRate(100);
        event.setAccuracy(4);
        _bus.post(event);
        timeout(2000).times(1);

        time += 2; // too early (dt<5s), do nothing
        event.setTime(time);
        _bus.post(event);
        timeout(2000).times(1);

        time += 10; // too early (5s<dt<30s), save point to send it later
        event.setTime(time);
        _bus.post(event);
        timeout(2000).times(1);


        time += 60;
        event.setLatitude(45.1);
        event.setLongitude(8.2);
        event.setTime(time);
        _bus.post(event);
        timeout(2000).times(1);

//        verify(_mockMessageManager, timeout(2000).times(1)).offer(captor.capture());

    }

    @SmallTest
    public void testLiveTrackingGetMsgLiveShort() throws Exception {
        LiveTracking liveTracking = new LiveTracking(LiveTracking.TYPE_JAYPS, _bus);
        Location location = new Location("PebbleBike");
        location.setAccuracy(5);
        location.setLatitude(48);
        location.setLongitude(3);
        location.setTime(1420980000);
        byte[] msgLiveShort = liveTracking.getMsgLiveShort(location);

        String[] names = liveTracking.getNames();
    }

    @SmallTest
    public void testLiveTrackingFriendSetFromNodeList() throws Exception {
        LiveTracking liveTracking = new LiveTracking(LiveTracking.TYPE_JAYPS, _bus);
        Location location = new Location("PebbleBike");
        location.setAccuracy(5);
        location.setLatitude(48);
        location.setLongitude(3);
        location.setTime(1420980000);

        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<message>"
          + "<type>activity_updated</type>"
          + "<friends>"
                + "<friend id=\"0\">"
                + "<id>f1/0</id>"
                + "<nickname>Friend 1</nickname>"
                + "<lat>44</lat>"
                + "<lon>-5</lon>"
                + "<ts>1421075656</ts>"
                + "</friend>"
                + "<friend id=\"1\">"
                + "<id>f2</id>"
                + "<nickname>Friend 2</nickname>"
                + "<lat>44</lat>"
                + "<lon>8</lon>"
                + "<ts>1421175938</ts>"
                + "</friend>"
                + "</friends>"
                + "<points>"
                + "<point id=\"0\">"
                + "<lat>49</lat>"
                + "<lon>7.7931</lon>"
                + "<ele>0.0</ele>"
                + "<ts>1421175944</ts>"
                + "<accuracy>10.0</accuracy>"
                + "</point>"
                + "<point id=\"1\">"
                + "<lat>49</lat>"
                + "<lon>7.7932</lon>"
                + "<ele>0.0</ele>"
                + "<ts>1421175963</ts>"
                + "<accuracy>10.0</accuracy>"
                + "</point>"
                + "</points>"
                + "</message>";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new ByteArrayInputStream(response.getBytes("utf-8"))));

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "";
        NodeList nodes;
        Node node;
        expression = "//friend";
        nodes = (NodeList)xpath.evaluate(expression, doc, XPathConstants.NODESET);


        HashMap<String, LiveTrackingFriend> friends = new HashMap<String, LiveTrackingFriend>();
        for( int i = 0; i < nodes.getLength(); i++ ) {
            // for each friends
            node = nodes.item(i);

            LiveTrackingFriend friend = new LiveTrackingFriend();
            friend.setFromNodeList(node.getChildNodes());

            if (friend.id != "" && friend.lat != null && friend.lon != null) {
                //nbReceivedFriends++;

                LiveTrackingFriend f2 = new LiveTrackingFriend();
                f2.updateFromFriend(friend, location);
                friends.put(friend.id, f2);
            }
        }
    }
}
