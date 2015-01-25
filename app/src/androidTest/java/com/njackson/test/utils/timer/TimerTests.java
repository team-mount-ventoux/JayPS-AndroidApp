package com.njackson.test.utils.timer;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.utils.time.ITimerHandler;
import com.njackson.utils.time.Timer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Created by njackson on 03/01/15.
 */
public class TimerTests extends AndroidTestCase {

    private Timer _timer;
    private ITimerHandler _mockHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        _mockHandler = mock(ITimerHandler.class);
        _timer = new Timer();
    }

    @SmallTest
    public void testWhenTimeoutHandlerCalled() throws InterruptedException {
        _timer.setTimer(500, _mockHandler);

        verify(_mockHandler, timeout(2000).times(1)).handleTimeout();
    }

    @SmallTest
    public void testWhenCanceledAndTimerSetReturnsTrue() throws InterruptedException {
        _timer.setTimer(500, _mockHandler);
        boolean canceled = _timer.cancel();

        assertTrue(canceled);
    }

    @SmallTest
    public void testWhenCanceledAndTimerNotSetReturnsFalse() throws InterruptedException {
        boolean canceled = _timer.cancel();

        assertFalse(canceled);
    }

    @SmallTest
    public void testWhenStartedSetsActiveToTrue() throws InterruptedException {
        _timer.setTimer(500, _mockHandler);

        assertTrue(_timer.getActive());
    }

    @SmallTest
    public void testWhenCanceledSetsActiveToFalse() throws InterruptedException {
        _timer.setTimer(500, _mockHandler);
        boolean canceled = _timer.cancel();

        assertFalse(_timer.getActive());
    }

    @SmallTest
    public void testWhenTimeoutSetsActiveToFalse() throws InterruptedException {
        _timer.setTimer(500, _mockHandler);

        verify(_mockHandler,timeout(2000).times(1)).handleTimeout();
        assertFalse(_timer.getActive());
    }

    @SmallTest
    public void testRepeatingWhenTimeoutSetsActiveToTrue() throws InterruptedException {
        _timer.setRepeatingTimer(500, _mockHandler);

        verify(_mockHandler,timeout(2000).times(1)).handleTimeout();
        assertTrue(_timer.getActive());
    }

    @SmallTest
    public void testRepeatingWhenStartedSetsActiveToTrue() throws InterruptedException {
        _timer.setRepeatingTimer(500, _mockHandler);

        assertTrue(_timer.getActive());
    }

    @SmallTest
    public void testRepeatingTimerCallsMultipleTimes() throws InterruptedException {
        _timer.setRepeatingTimer(500, _mockHandler);

        verify(_mockHandler,timeout(2000).times(2)).handleTimeout();
    }
}
