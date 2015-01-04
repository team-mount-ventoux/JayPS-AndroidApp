package com.njackson.test;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.test.InstrumentationTestRunner;
import android.util.Log;

import com.njackson.test.application.TestApplication;

/**
 * Created by server on 30/03/2014.
 */
public class MyInstrumentationTestRunner extends InstrumentationTestRunner{

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return Instrumentation.newApplication(TestApplication.class, context);
    }

}
