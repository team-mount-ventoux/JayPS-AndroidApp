package oruxmaps;

import android.content.Intent;
import android.content.SharedPreferences;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.gps.GPSService;
import com.njackson.oruxmaps.IOruxMaps;
import com.njackson.oruxmaps.OruxMapsService;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.time.ITime;
import com.squareup.otto.Bus;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 17/01/15.
 */
public class OruxMapsServiceTest extends ServiceTestCase<OruxMapsService> {

    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;

    private OruxMapsService _service;

    private static IOruxMaps _mockOruxMaps;
    private static ITime _mockTime;

    @Module(
            includes = PebbleBikeModule.class,
            injects = OruxMapsServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides IOruxMaps providesOruxMaps() { return _mockOruxMaps; }
        @Provides @Singleton SharedPreferences providesSharedPreferences() { return mock(SharedPreferences.class); }
        @Provides ITime providesTime() { return _mockTime; }
    }


    /**
     * Constructor
     *
     * @param serviceClass The type of the service under test.
     */
    public OruxMapsServiceTest(Class<OruxMapsService> serviceClass) {
        super(serviceClass);
    }

    public OruxMapsServiceTest() {
        super(OruxMapsService.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getSystemContext().getCacheDir().getPath());
        setupMocks();

        TestApplication app = new TestApplication();
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);
        _bus.register(this);

        setApplication(app);

    }

    private void setupMocks() {
        _mockOruxMaps = mock(IOruxMaps.class);
        _mockTime = mock(ITime.class);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), GPSService.class);
        startService(startIntent);
        _service = getService();
    }

    @SmallTest
    public void testReturnsNullOnBind() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("disable");
        startService();

        assertNull(_service.onBind(new Intent()));
    }

    @SmallTest
    public void testContinueOruxMapsOnStartWhenPreferenceContinue() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("continue");

        startService();

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordContinue();
    }

    @SmallTest
    public void testNewSegmentOruxMapsOnStartWhenPreferenceNewSegment() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("new_segment");

        startService();

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewSegment();
    }

    @SmallTest
    public void testNewTrackOruxMapsOnStartWhenPreferenceNewTrack() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("new_track");

        startService();

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewTrack();
    }

    @SmallTest
    public void testNewTrackOruxMapsOnStartWhenPreferenceAutoAndLastStart12HoursAgo() throws Exception {
        when(_sharedPreferences.getLong("GPS_LAST_START", 0)).thenReturn((long)0);
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn((long)((12 * 3600 * 1000) + 1));
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("auto");

        startService();

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewTrack();
    }

    @SmallTest
    public void testNewSegmentOruxMapsOnStartWhenPreferenceAutoAndLastStartLess12HoursAgo() throws Exception {
        when(_sharedPreferences.getLong("GPS_LAST_START", 0)).thenReturn((long)0);
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn((long)12);
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("auto");

        startService();

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewSegment();
    }

    @SmallTest
    public void testSavesOruxMapsOnDestroyWhenPreferenceSet() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("something");

        startService();
        shutdownService();

        verify(_mockOruxMaps,timeout(2000).times(1)).stopRecord();
    }

    @SmallTest
    public void testDoesNotSaveOruxMapsOnDestroyWhenPreferenceDisabled() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("disable");

        startService();
        shutdownService();

        verify(_mockOruxMaps,timeout(2000).times(0)).stopRecord();
    }

}
