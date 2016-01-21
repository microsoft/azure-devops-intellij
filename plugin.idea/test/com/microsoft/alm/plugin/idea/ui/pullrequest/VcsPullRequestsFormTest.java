// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class VcsPullRequestsFormTest extends IdeaAbstractTest {
    VcsPullRequestsForm underTest;

    @Before
    public void setUp() {
        underTest = new VcsPullRequestsForm();
    }

    @Test
    public void testNotConnected() {
        underTest.setConnectionStatus(false /*connected*/, false /*authenticating*/, false /*authenticated*/, false /*loading*/);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_CONNECTED), underTest.getStatusText());
        assertEquals(underTest.getStatusLinkText(), TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));

        underTest.setConnectionStatus(false, true, true, true);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_CONNECTED), underTest.getStatusText());
        assertEquals(underTest.getStatusLinkText(), TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
    }

    @Test
    public void testAuthenticating() {
        underTest.setConnectionStatus(true /*connected*/, true /*authenticating*/, false /*authenticated*/, false /*loading*/);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_AUTHENTICATING), underTest.getStatusText());
        assertEquals("", underTest.getStatusLinkText());
    }

    @Test
    public void testNotAuthenticated() {
        underTest.setConnectionStatus(true /*connected*/, false /*authenticating*/, false /*authenticated*/, false /*loading*/);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_AUTHENTICATED), underTest.getStatusText());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SIGN_IN), underTest.getStatusLinkText());

        underTest.setConnectionStatus(true /*connected*/, false /*authenticating*/, false /*authenticated*/, true /*loading*/);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_AUTHENTICATED), underTest.getStatusText());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SIGN_IN), underTest.getStatusLinkText());
    }

    @Test
    public void pullRequestsLoading() {
        underTest.setConnectionStatus(true /*connected*/, false /*authenticating*/, true /*authenticated*/, true /*loading*/);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING), underTest.getStatusText());
        assertEquals("", underTest.getStatusLinkText());
    }

    @Test
    public void loadingComplete() {
        final Date loadingTime = new Date();
        underTest.setLastRefreshed(loadingTime);
        underTest.setConnectionStatus(true, false, true, false);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LAST_REFRESHED_AT, loadingTime.toString()), underTest.getStatusText());
        assertEquals(TfPluginBundle.message((TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER)), underTest.getStatusLinkText());
    }
}
