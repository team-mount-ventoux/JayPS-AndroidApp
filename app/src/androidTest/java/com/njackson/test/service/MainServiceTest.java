package com.njackson.test.service;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.modules.AndroidModule;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.service.IServiceCommand;
import com.njackson.service.MainService;
import com.njackson.test.application.TestApplication;

import java.util.Arrays;
import java.util.List;

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
    private IServiceCommand _mockServiceCommand;

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

        verify(_mockServiceStarter,timeout(2000).times(1)).startServiceForeground(any(MainService.class),anyString(),anyString());
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
}
