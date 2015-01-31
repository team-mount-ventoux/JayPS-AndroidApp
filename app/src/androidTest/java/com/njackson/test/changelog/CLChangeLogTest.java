package com.njackson.test.changelog;

import android.app.AlertDialog;
import android.test.AndroidTestCase;

import com.njackson.changelog.CLChangeLog;

import de.cketti.library.changelog.ChangeLog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 31/01/15.
 */
public class CLChangeLogTest extends AndroidTestCase {

    private ChangeLog _mockChangeLog;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _mockChangeLog = mock(ChangeLog.class);
    }

    public void testWhenIsFirstRunCalledCallsChangeLog() {
        when(_mockChangeLog.isFirstRun()).thenReturn(true);

        CLChangeLog clChangeLog = new CLChangeLog(_mockChangeLog);

        assertTrue(clChangeLog.isFirstRun());
        verify(_mockChangeLog,times(1)).isFirstRun();
    }

    public void testWhenGetDialogCalledCallsChangeLog() {
        when(_mockChangeLog.getLogDialog()).thenReturn(mock(AlertDialog.class));

        CLChangeLog clChangeLog = new CLChangeLog(_mockChangeLog);

        assertNotNull(clChangeLog.getDialog());
        verify(_mockChangeLog,times(1)).getLogDialog();
    }

}
