package com.njackson.test.pebble;

import android.content.Context;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.R;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.PebbleService.NewMessage;
import com.njackson.oruxmaps.IOruxMaps;
import com.njackson.pebble.IMessageManager;
import com.njackson.pebble.PebbleDataReceiver;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.services.IServiceStarter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 17/01/15.
 */
public class PebbleDataReceiverTest extends AndroidTestCase {

    @Inject Bus _bus;
    @Inject IServiceStarter _mockServiceStarter;

    private static IMessageManager _mockMessageManager;
    private static IOruxMaps _mockOruxMaps;
    private Resources _mockResource;
    private Context _mockContext;
    private TestApplication _app;
    private PebbleDataReceiver _pebbleDataReceiver;
    private ResetGPSState _refreshEvent;
    private NewMessage _messageEvent;
    private CountDownLatch _stateLatch;
    private CountDownLatch _messageLatch;


    @Module(
            includes = PebbleBikeModule.class,
            injects = PebbleDataReceiverTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides @Singleton
        IServiceStarter provideServiceStarter() { return mock(IServiceStarter.class); }

        @Provides
        public IMessageManager providesMessageManager() { return _mockMessageManager; }

        @Provides
        IOruxMaps providesOruxMaps() { return _mockOruxMaps; }
    }

    @Subscribe
    public void onResetGPSState(ResetGPSState state) {
        _refreshEvent = state;
        _stateLatch.countDown();
    }

    @Subscribe
    public void onNewMessage(NewMessage event) {
        _messageEvent = event;
        _messageLatch.countDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);
        _bus.register(this);

        setupMocks();

        _pebbleDataReceiver = new PebbleDataReceiver();
        _stateLatch = new CountDownLatch(1);
        _messageLatch = new CountDownLatch(1);
    }

    private void setupMocks() {
        _mockMessageManager = mock(IMessageManager.class);
        _mockOruxMaps = mock(IOruxMaps.class);

        _mockResource = mock(Resources.class);

        _mockContext = mock(Context.class);
        when(_mockContext.getResources()).thenReturn(_mockResource);
        when(_mockContext.getApplicationContext()).thenReturn(_app);
    }

    @SmallTest
    public void testReceiveDataAcknowledgesMessage() {
        PebbleDictionary dic = new PebbleDictionary();
        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        verify(_mockMessageManager,times(1)).sendAckToPebble(12345);
    }

    @SmallTest
    public void testReceiveDataWithORUXMAPS_START_RECORD_CONTINUE_PRESSStartsRecord() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addUint32(Constants.CMD_BUTTON_PRESS, Constants.ORUXMAPS_START_RECORD_CONTINUE_PRESS);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        verify(_mockOruxMaps,times(1)).startRecordNewSegment();
    }

    @SmallTest
    public void testReceiveDataWithORUXMAPS_STOP_RECORD_PRESSStartsRecord() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addUint32(Constants.CMD_BUTTON_PRESS, Constants.ORUXMAPS_STOP_RECORD_PRESS);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        verify(_mockOruxMaps,times(1)).stopRecord();
    }

    @SmallTest
    public void testReceiveDataWithORUXMAPS_NEW_WAYPOINT_PRESSAddsNewWaypoint() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addUint32(Constants.CMD_BUTTON_PRESS, Constants.ORUXMAPS_NEW_WAYPOINT_PRESS);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        verify(_mockOruxMaps,times(1)).newWaypoint();
    }

    @SmallTest
    public void testReceiveDataWithSTOP_PRESSStopsLocationServices() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addUint32(Constants.CMD_BUTTON_PRESS, Constants.STOP_PRESS);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        verify(_mockServiceStarter,times(1)).stopLocationServices();
    }

    @SmallTest
    public void testReceiveDataWithPLAY_PRESSStartsLocationServices() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addUint32(Constants.CMD_BUTTON_PRESS, Constants.PLAY_PRESS);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        verify(_mockServiceStarter,times(1)).startLocationServices();
    }

    @SmallTest
    public void testReceiveDataWithREFRESH_PRESSRefreshLocationServices() throws InterruptedException {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addUint32(Constants.CMD_BUTTON_PRESS, Constants.REFRESH_PRESS);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        _stateLatch.await(1000, TimeUnit.MILLISECONDS);
        assertNotNull(_refreshEvent);
    }

    @SmallTest
    public void testReceiveDataWithMSG_VERSION_PEBBLEAndVersionGreaterOrEqualThanCurrentDoesNotSendMessage() throws InterruptedException {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addInt32(Constants.MSG_VERSION_PEBBLE, Constants.LAST_VERSION_PEBBLE);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        _messageLatch.await(500, TimeUnit.MILLISECONDS);
        assertNull(_messageEvent);
    }

    @SmallTest
    public void testReceiveDataWithMSG_VERSION_PEBBLEAndVersionLessThanCurrentSendsMessage() throws InterruptedException {
        when(_mockResource.getString(R.string.message_pebble_new_watchface)).thenReturn("some message");

        PebbleDictionary dic = new PebbleDictionary();
        dic.addInt32(Constants.MSG_VERSION_PEBBLE, Constants.LAST_VERSION_PEBBLE - 1);

        _pebbleDataReceiver.receiveData(_mockContext,12345,dic);

        _messageLatch.await(500, TimeUnit.MILLISECONDS);
        assertEquals("some message", _messageEvent.getMessage());
    }

}
