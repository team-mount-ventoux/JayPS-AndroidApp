package com.njackson.test.utils.timer;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.utils.timer.ITimerHandler;
import com.njackson.utils.timer.Timer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by njackson on 03/01/15.
 */
public class TimerTests extends AndroidTestCase implements ITimerHandler{

    private Timer _timer;
    private CountDownLatch _latch;
    private boolean _handlerCalled;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        _timer = new Timer();
        _latch = new CountDownLatch(1);
        _handlerCalled = false;
    }

    @SmallTest
    public void testWhenTimeoutHandlerCalled() throws InterruptedException {
        _timer.setTimer(500,this);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(_handlerCalled);
    }

    @SmallTest
    public void testWhenCanceledAndTimerSetReturnsTrue() throws InterruptedException {
        _timer.setTimer(500,this);
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
        _timer.setTimer(500,this);

        assertTrue(_timer.getActive());
    }

    @SmallTest
    public void testWhenCanceledSetsActiveToFalse() throws InterruptedException {
        _timer.setTimer(500,this);
        boolean canceled = _timer.cancel();

        assertFalse(_timer.getActive());
    }

    @SmallTest
    public void testWhenTimeoutSetsActiveToFalse() throws InterruptedException {
        _timer.setTimer(500,this);
        _latch.await(2000, TimeUnit.MILLISECONDS);

        assertFalse(_timer.getActive());
    }

    @Override
    public void handleTimeout() {
        _latch.countDown();
        _handlerCalled = true;
    }
}
