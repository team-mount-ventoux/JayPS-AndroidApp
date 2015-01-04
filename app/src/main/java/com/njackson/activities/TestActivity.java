package com.njackson.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.njackson.R;

/**
 * Created by server on 27/04/2014.
 */
public class TestActivity extends FragmentActivity {
    private static final String TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        Log.d(TAG,"OnCreate");
        setContentView(R.layout.activity_test);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG,"Resume");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG,"Pause");
    }
}