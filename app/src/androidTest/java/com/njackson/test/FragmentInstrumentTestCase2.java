package com.njackson.test;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.test.ActivityInstrumentationTestCase2;

import com.njackson.R;
import com.njackson.activities.TestActivity;

/**
 * Created by server on 27/04/2014.
 */
public class FragmentInstrumentTestCase2 extends ActivityInstrumentationTestCase2<TestActivity> {
    public FragmentInstrumentTestCase2() { super(TestActivity.class); }

    protected TestActivity _activity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
    }

    protected Fragment startFragment(Fragment fragment) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.activity_test_fragment_linearlayout, fragment, "tag");
        transaction.commit();
        getInstrumentation().waitForIdleSync();
        Fragment frag = _activity.getSupportFragmentManager().findFragmentByTag("tag");
        return frag;
    }

}
