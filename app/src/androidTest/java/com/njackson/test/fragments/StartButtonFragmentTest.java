package com.njackson.test.fragments;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.Button;

import com.njackson.R;
import com.njackson.application.modules.AndroidModule;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.test.FragmentInstrumentTestCase2;
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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 27/04/2014.
 */
public class StartButtonFragmentTest extends FragmentInstrumentTestCase2 {

    @Inject Bus _bus;
    @Inject IServiceStarter _serviceStarter;

    private Button _button;

    private StartButtonTouchedEvent _startButtonEvent;
    private StopButtonTouchedEvent _stopButtonEvent;

    private CountDownLatch _startLatch;
    private CountDownLatch _stopLatch;

    @Module(
            includes = AndroidModule.class,
            injects = StartButtonFragmentTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        LocationManager provideLocationManager() {
            return mock(LocationManager.class);
        }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }

        @Provides @Singleton
        IServiceStarter provideServiceStarter() { return mock(IServiceStarter.class); }
    }

    @Subscribe
    public void onStartButtonTouched(StartButtonTouchedEvent event) {
        _startButtonEvent = event;
        _startLatch.countDown();
    }
    @Subscribe
    public void onStopButtonTouched(StopButtonTouchedEvent event) {
        _stopButtonEvent = event;
        _stopLatch.countDown();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.getInstrumentation().waitForIdleSync(); // this is needed for emulator versions 2.3 as the application is instantiated on a separate thread.
        TestApplication app = (TestApplication)this.getInstrumentation().getTargetContext().getApplicationContext();

        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        _activity = getActivity();
        _bus.register(this);

        _startLatch = new CountDownLatch(1);
        _stopLatch = new CountDownLatch(1);
    }

    @SmallTest
    public void test_FragmentSetupCorrectly() throws InterruptedException {
        startFragment(new StartButtonFragment());

        _button = (Button) _activity.findViewById(R.id.start_button);
        assertNotNull(_button);
    }

    @SmallTest
    public void testChecksServiceStateOnResume() {
        startFragment(new StartButtonFragment());

        verify(_serviceStarter,timeout(2000).times(1)).broadcastLocationState();
    }

    @SmallTest
    public void test_StartButtonFiresStartEventWhenTouchedAndTextIsSTART() throws InterruptedException {
        startFragment(new StartButtonFragment());

        _button = (Button) _activity.findViewById(R.id.start_button);

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _button.performClick();
            }
        });

        _startLatch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull("Start button event should not be null", _startButtonEvent);
    }

    @SmallTest
    public void test_StartButtonFiresStartEventWhenTouchedAndTextIsSTOP() throws InterruptedException {
        startFragment(new StartButtonFragment());

        _button = (Button) _activity.findViewById(R.id.start_button);
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _button.setText(_activity.getString(R.string.startbuttonfragment_stop));
                _button.performClick();
            }
        });

        _stopLatch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull("Start button event should not be null",_stopButtonEvent);
    }

    @SmallTest
    public void test_GPSStartEventChangesTextToStartWhenEventReceived() throws InterruptedException {
        startFragment(new StartButtonFragment());
        _button = (Button) _activity.findViewById(R.id.start_button);
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _button.setText(R.string.startbuttonfragment_stop);
            }
        });

        _bus.post(new GPSStatus(BaseStatus.Status.STOPPED));

        Thread.sleep(100);
        assertEquals("Start button text should be start",_activity.getString(R.string.startbuttonfragment_start),_button.getText());
    }

    @SmallTest
    public void test_GPSStopEventChangesTextToStopWhenEventReceived() throws InterruptedException {
        startFragment(new StartButtonFragment());
        _button = (Button) _activity.findViewById(R.id.start_button);
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _button.setText(R.string.startbuttonfragment_start);
            }
        });

        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));

        Thread.sleep(100);
        assertEquals("Start button text should be stop",_activity.getString(R.string.startbuttonfragment_stop),_button.getText());
    }
}
