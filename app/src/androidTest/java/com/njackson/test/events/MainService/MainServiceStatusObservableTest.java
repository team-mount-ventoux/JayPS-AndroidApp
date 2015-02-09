package com.njackson.test.events.MainService;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.events.MainService.MainServiceStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.events.rx.MainServiceStatusObservable;
import com.squareup.otto.Bus;

import java.util.Arrays;

import rx.observers.TestSubscriber;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by njackson on 09/02/15.
 */
public class MainServiceStatusObservableTest extends AndroidTestCase {

    private Bus _mockBus;
    private TestSubscriber<BaseStatus.Status> _testSubscriber;
    private MainServiceStatusObservable _sut;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().toString());
        setupMocks();


        _sut = new MainServiceStatusObservable(_mockBus);
        _testSubscriber = new TestSubscriber<>();
    }

    private void setupMocks() {
        _mockBus = mock(Bus.class);
    }

    @SmallTest
    public void test_RegistersWithBusOnCall() {
        _sut.call(_testSubscriber);

        verify(_mockBus,times(1)).register(_sut);
    }

    @SmallTest
    public void test_UnRegistersWithBusOnUnsubscribe() {
        _sut.call(_testSubscriber);
        _testSubscriber.unsubscribe();

        verify(_mockBus, times(1)).unregister(_sut);
    }

    @SmallTest
    public void test_BroadcastsStatusOnNewEvent() {
        _sut.call(_testSubscriber);
        _sut.onMainServiceStatus(new MainServiceStatus(BaseStatus.Status.STARTED));

        _testSubscriber.assertReceivedOnNext(Arrays.asList(BaseStatus.Status.STARTED));
    }

}
