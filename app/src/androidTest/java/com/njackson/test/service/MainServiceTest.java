package com.njackson.test.service;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.gps.GPSService;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.service.MainService;
import com.njackson.test.application.TestApplication;
import com.njackson.test.testUtils.Services;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Created by njackson on 24/01/15.
 */
public class MainServiceTest extends ServiceTestCase<MainService> {

    private static IForegroundServiceStarter _mockServiceStarter;
    private MainService _service;

    @Module(
            includes = PebbleBikeModule.class,
            injects = MainServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        IForegroundServiceStarter providesForegroundServiceStarter() { return _mockServiceStarter; }
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
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);

        setApplication(app);

        setupMocks();
    }

    private void setupMocks() {
        _mockServiceStarter = mock(IForegroundServiceStarter.class);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), MainService.class);
        startService(startIntent);
        _service = getService();
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

        verify(_mockServiceStarter,timeout(2000).times(1)).startServiceForeground(any(GPSService.class),anyString(),anyString());
    }

    @SmallTest
    public void testStopsServiceForeground() throws Exception {
        startService();
        shutdownService();

        verify(_mockServiceStarter,timeout(2000).times(1)).stopServiceForeground(any(GPSService.class));
    }
}
