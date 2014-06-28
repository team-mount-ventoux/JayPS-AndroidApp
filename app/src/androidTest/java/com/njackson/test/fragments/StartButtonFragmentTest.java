package com.njackson.test.fragments;

import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;

import com.njackson.R;
import com.njackson.application.PebbleBikeModule;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.test.FragmentInstrumentTestCase2;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;

/**
 * Created by server on 27/04/2014.
 */
public class StartButtonFragmentTest extends FragmentInstrumentTestCase2 {

    @Inject Bus _bus;

    private Button _button;

    private StartButtonTouchedEvent _startButtonEvent;
    private StopButtonTouchedEvent _stopButtonEvent;

    @Module(
            includes = PebbleBikeModule.class,
            injects = StartButtonFragmentTest.class,
            overrides = true
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

        startFragment(new StartButtonFragment());

        _button = (Button) _activity.findViewById(R.id.start_button);
        assertNotNull(_button);
        assertEquals("Start button text should be start",_activity.getString(R.string.startbuttonfragment_start),_button.getText());
    }

    @Subscribe
    public void onStartButtonTouched(StartButtonTouchedEvent event) {
        _startButtonEvent = event;
    }
    @Subscribe
    public void onStopButtonTouched(StopButtonTouchedEvent event) {
        _stopButtonEvent = event;
    }

    @MediumTest
    @UiThreadTest
    public void test_StartButtonFiresStartEventWhenTouchedAndTextIsSTART() throws InterruptedException {
        Button button = (Button) _activity.findViewById(R.id.start_button);
        button.performClick();

        Thread.sleep(100);
        assertNotNull("Start button event should not be null",_startButtonEvent);
    }

    @MediumTest
    @UiThreadTest
    public void test_StartButtonFiresStartEventWhenTouchedAndTextIsSTOP() throws InterruptedException {
        Button button = (Button) _activity.findViewById(R.id.start_button);
        _button.setText(_activity.getString(R.string.startbuttonfragment_stop));
        button.performClick();

        Thread.sleep(100);
        assertNotNull("Start button event should not be null",_stopButtonEvent);
    }

    @MediumTest
    @UiThreadTest
    public void test_StartButtonChangesTextToStopWhenTouched() throws InterruptedException {
        _button.setText(_activity.getString(R.string.startbuttonfragment_start));
        _button.performClick();

        Thread.sleep(100);
        assertEquals("Start button text should be stop",_activity.getString(R.string.startbuttonfragment_stop),_button.getText());
    }

    @MediumTest
    @UiThreadTest
    public void test_StartButtonChangesTextToStartWhenTouched() throws InterruptedException {
        _button.setText(R.string.startbuttonfragment_stop);
        _button.performClick();

        Thread.sleep(100);
        assertEquals("Start button text should be start",_activity.getString(R.string.startbuttonfragment_start),_button.getText());
    }

}
