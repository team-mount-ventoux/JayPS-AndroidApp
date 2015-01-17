package oruxmaps;

import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.oruxmaps.OruxMaps;

import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by njackson on 17/01/15.
 */
public class OruxMapsTest extends AndroidTestCase {

    private Context _mockContext;
    private OruxMaps _oruxMaps;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        _mockContext = mock(Context.class);
        _oruxMaps = new OruxMaps(_mockContext);
    }

    @SmallTest
    public void testStartRecordNewSegmentSendsIntent() {
        _oruxMaps.startRecordNewSegment();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("com.oruxmaps.INTENT_START_RECORD_NEWSEGMENT", intentArgumentCaptor.getValue().getAction());
    }

    @SmallTest
    public void testStartRecordNewTrackSendsIntent() {
        _oruxMaps.startRecordNewTrack();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("com.oruxmaps.INTENT_START_RECORD_NEWTRACK", intentArgumentCaptor.getValue().getAction());
    }

    @SmallTest
    public void testStartRecordContinueSendsIntent() {
        _oruxMaps.startRecordContinue();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("com.oruxmaps.INTENT_START_RECORD_CONTINUE", intentArgumentCaptor.getValue().getAction());
    }

    @SmallTest
    public void testStopRecordSendsIntent() {
        _oruxMaps.stopRecord();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("com.oruxmaps.INTENT_STOP_RECORD", intentArgumentCaptor.getValue().getAction());
    }

    @SmallTest
    public void testNewWaypointSendsIntent() {
        _oruxMaps.newWaypoint();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("com.oruxmaps.INTENT_NEW_WAYPOINT", intentArgumentCaptor.getValue().getAction());
    }

}
