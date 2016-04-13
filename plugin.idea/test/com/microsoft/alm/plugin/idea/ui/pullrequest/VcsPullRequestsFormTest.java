// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VcsPullRequestsFormTest extends IdeaAbstractTest {
    VcsPullRequestsForm underTest;

    @Before
    public void setUp() {
        underTest = new VcsPullRequestsForm();
    }

    @Test
    public void testNotConnected() {
        underTest.setStatus(VcsTabStatus.NOT_TF_GIT_REPO);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_CONNECTED), underTest.getStatusText());
        assertEquals(underTest.getStatusLinkText(), TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
    }

    @Test
    public void testNotAuthenticated() {
        underTest.setStatus(VcsTabStatus.NO_AUTH_INFO);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_AUTHENTICATED), underTest.getStatusText());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SIGN_IN), underTest.getStatusLinkText());
    }

    @Test
    public void pullRequestsLoading() {
        underTest.setStatus(VcsTabStatus.LOADING_IN_PROGRESS);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING), underTest.getStatusText());
        assertEquals("", underTest.getStatusLinkText());
    }

    @Test
    public void loadingComplete() {
        underTest.setStatus(VcsTabStatus.LOADING_COMPLETED);
        assertTrue(underTest.getStatusText().startsWith(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LAST_REFRESHED_AT, "")));
        assertEquals(TfPluginBundle.message((TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER)), underTest.getStatusLinkText());
    }

    @Test
    public void loadingErrors() {
        underTest.setStatus(VcsTabStatus.LOADING_COMPLETED_ERRORS);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING_ERRORS), underTest.getStatusText());
        assertEquals(TfPluginBundle.message((TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER)), underTest.getStatusLinkText());
    }
}
