package com.njackson.test.service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.modules.AndroidModule;
import com.njackson.events.MainService.MainServiceStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.service.IServiceCommand;
import com.njackson.service.MainService;
import com.njackson.test.application.TestApplication;
import com.njackson.test.testUtils.Services;
import com.njackson.utils.time.ITimer;
import com.squareup.otto.Bus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 24/01/15.
 */
public class MainServiceTest extends ServiceTestCase<MainService> {

    private static IForegroundServiceStarter _mockServiceStarter;
    private MainService _service;
    private MainService _serviceSpy;
    private IServiceCommand _mockServiceCommand;
    private ITimer _mockTimer;

    @Inject Bus _mockBus;

    @Module(
            includes = AndroidModule.class,
            injects = MainServiceTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {

        @Provides
        IForegroundServiceStarter providesForegroundServiceStarter() { return _mockServiceStarter; }
        @Provides
        List<IServiceCommand> providesServiceCommands() {
            return Arrays.asList(
                    _mockServiceCommand,
                    _mockServiceCommand,
                    _mockServiceCommand
            );
        }

        @Provides
        ITimer providesTimer() {
            return _mockTimer;
        }

        @Singleton
        @Provides
        Bus providesBus() {
            return mock(Bus.class);
        }
    }

    /**
     * Constructor
     *
     * @param serviceClass The type of the service under test.
     */
    public MainServiceTest(Class<MainService> serviceClass) {
        super(serviceClass);
    }
    public MainServiceTest() {
        super(MainService.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getSystemContext().getCacheDir().getPath());

        TestApplication app = new TestApplication();
        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        setApplication(app);

        setupMocks();
    }

    private void setupMocks() {
        _mockServiceStarter = mock(IForegroundServiceStarter.class);
        _mockServiceCommand = mock(IServiceCommand.class);
        _mockTimer = mock(ITimer.class);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), MainService.class);
        startService(startIntent);
        _service = getService();
        _serviceSpy = spy(_service);

    }

    @SmallTest
    public void testOnBindReturnsNull() throws Exception {
        startService();
        IBinder binder = _service.onBind(new Intent());

        assertNull(binder);
    }

    @SmallTest
    public void testStartsServiceForeground() throws Exception {
        startService();

        verify(_mockServiceStarter,timeout(2000).times(1)).startServiceForeground(any(MainService.class),anyString(),anyString(),anyInt());
    }

    @SmallTest
    public void testStopsServiceForeground() throws Exception {
        startService();
        shutdownService();

        verify(_mockServiceStarter,timeout(2000).times(1)).stopServiceForeground(any(MainService.class));
    }

    @SmallTest
    public void testSetsUpCommandsOnStart() throws Exception {
        startService();

        verify(_mockServiceCommand,timeout(1000).times(3)).execute(any(PebbleBikeApplication.class));
    }

    @SmallTest
    public void testSetsUpAutoStop() throws Exception {
        startService();

        verify(_mockTimer,timeout(2000).times(1)).setRepeatingTimer(1000, _service);
    }

    @SmallTest
    public void testBroadcastsStartedMessageOnCreate() throws Exception {
        startService();

        verify(_mockBus,timeout(1000).times(1)).post(any(MainServiceStatus.class));
    }

    @SmallTest
    public void testStopsAutomaticallyWhenNoCommandsRunning() throws Exception {
        startService();

        _service.handleTimeout();

        verify(_mockBus,timeout(1000).times(2)).post(any(MainServiceStatus.class));
    }

    @SmallTest
    public void testCancelsTimerWhenNoCommandsRunning() throws Exception {
        startService();

        _service.handleTimeout();

        verify(_mockTimer,timeout(1000).times(1)).cancel();
    }

    @SmallTest
    public void testDoesNotStopAutomaticallyWhenCommandsRunning() throws Exception {
        when(_mockServiceCommand.getStatus()).thenReturn(BaseStatus.Status.STARTED);
        startService();

        _service.handleTimeout();

        verify(_mockBus,timeout(1000).times(1)).post(any(MainServiceStatus.class));
    }

    @SmallTest
    public void testDisposeCommandsOnDestroy() throws Exception {
        startService();
        shutdownService();

        verify(_mockServiceCommand,timeout(1000).times(3)).dispose();
    }
}
