package com.njackson.test.utils.pebble;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.utils.messages.IMessageMaker;
import com.njackson.utils.watchface.InstallPebbleWatchFace;
import com.njackson.utils.version.IAndroidVersion;
import com.njackson.utils.version.IWatchFaceVersion;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by njackson on 24/12/14.
 */
public class InstallWatchFaceTest extends AndroidTestCase {

    InstallPebbleWatchFace _install;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _install = new InstallPebbleWatchFace(mock(IAndroidVersion.class), mock(IWatchFaceVersion.class));
    }

    @SmallTest
    public void testGetDownloadUrlReturnsValidUri() {
        Uri uri = _install.getDownloadUrl("21", "2.1.1", "http://myurl.com/watchapp.pbw");

        assertEquals(uri.getHost(),"myurl.com");
    }
/* no longer using startupIntent.setComponent()
    @SmallTest
    public void testCreateIntentReturnsValidIntent() {
        Intent intent = _install.createIntent(null);

        assertEquals(intent.getComponent().getClassName(),"com.getpebble.android.ui.UpdateActivity");
        assertEquals(intent.getComponent().getPackageName(),"com.getpebble.android");
    }
*/
    @SmallTest
    public void testExecuteWhenApplicationInstalledStartsActivity() {
        Context mockContext = mock(Context.class);

        _install.execute(mockContext, null, "http://myurl.com/watchapp.pbw");

        verify(mockContext,times(1)).startActivity(any(Intent.class));
    }

    @SmallTest
    public void testExecuteWhenApplicationNotInstalledCreatesToast() {
        Context mockContext = mock(Context.class);
        IMessageMaker mockToast = mock(IMessageMaker.class);

        doThrow(new ActivityNotFoundException()).when(mockContext).startActivity(any(Intent.class));

        _install.execute(mockContext, mockToast, "http://myurl.com/watchapp.pbw");

        verify(mockToast, times(1)).showMessage(eq(mockContext), anyString());
    }

}

