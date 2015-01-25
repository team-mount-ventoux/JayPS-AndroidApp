package oruxmaps;

import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.modules.AndroidModule;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.oruxmaps.IOruxMaps;
import com.njackson.oruxmaps.OruxMapsServiceCommand;
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
public class OruxMapsServiceCommandTest extends AndroidTestCase {

    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;

    private OruxMapsServiceCommand _command;

    private static IOruxMaps _mockOruxMaps;
    private static ITime _mockTime;
    private TestApplication _app;

    @Module(
            includes = AndroidModule.class,
            injects = OruxMapsServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {
        @Provides IOruxMaps providesOruxMaps() { return _mockOruxMaps; }
        @Provides @Singleton SharedPreferences providesSharedPreferences() { return mock(SharedPreferences.class); }
        @Provides ITime providesTime() { return _mockTime; }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        setupMocks();

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(new TestModule()));
        _app.inject(this);
        _bus.register(this);

        _command = new OruxMapsServiceCommand();
    }

    private void setupMocks() {
        _mockOruxMaps = mock(IOruxMaps.class);
        _mockTime = mock(ITime.class);
    }

    @SmallTest
    public void testContinueOruxMapsOnGPSStartWhenPreferenceContinue() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("continue");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));

        verify(_mockOruxMaps, timeout(2000).times(1)).startRecordContinue();
    }

    @SmallTest
    public void testNewSegmentOruxMapsOnGPSStartWhenPreferenceNewSegment() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("new_segment");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));

        verify(_mockOruxMaps, timeout(2000).times(1)).startRecordNewSegment();
    }

    @SmallTest
    public void testNewTrackOruxMapsOnGPSStartWhenPreferenceNewTrack() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("new_track");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewTrack();
    }

    @SmallTest
    public void testNewTrackOruxMapsOnGPSStartWhenPreferenceAutoAndLastStart12HoursAgo() throws Exception {
        when(_sharedPreferences.getLong("GPS_LAST_START", 0)).thenReturn((long)0);
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn((long)((12 * 3600 * 1000) + 1));
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("auto");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewTrack();
    }

    @SmallTest
    public void testNewSegmentOruxMapsOnGPSStartWhenPreferenceAutoAndLastStartLess12HoursAgo() throws Exception {
        when(_sharedPreferences.getLong("GPS_LAST_START", 0)).thenReturn((long)0);
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn((long)12);
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("auto");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));

        verify(_mockOruxMaps,timeout(2000).times(1)).startRecordNewSegment();
    }

    @SmallTest
    public void testSavesOruxMapsOnGPSStopWhenPreferenceSet() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("something");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STOPPED));

        verify(_mockOruxMaps,timeout(2000).times(1)).stopRecord();
    }

    @SmallTest
    public void testDoesNotSaveOruxMapsOnGPSStopWhenPreferenceDisabled() throws Exception {
        when(_sharedPreferences.getString("ORUXMAPS_AUTO", "disable")).thenReturn("disable");

        _command.execute(_app);
        _bus.post(new GPSStatus(BaseStatus.Status.STOPPED));

        verify(_mockOruxMaps,timeout(2000).times(0)).stopRecord();
    }

}
