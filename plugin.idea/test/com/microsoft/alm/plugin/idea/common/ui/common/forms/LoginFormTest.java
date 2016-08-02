// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.forms;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the VSO and Tfs login forms
 */
public class LoginFormTest extends IdeaAbstractTest {

    @Test
    public void tfsLoginForm() {
        final TfsLoginForm form = new TfsLoginForm();

        //verify spinner is shown correctly when authenticating
        form.setAuthenticating(true);
        assertTrue(form.getBusySpinner().isVisible());
        form.setAuthenticating(false);
        assertFalse(form.getBusySpinner().isVisible());

        assertEquals("", form.getServerName()); //defaults to empty string for server name
        form.setServerUrl("http://myserver:8080/tfs ");
        assertEquals("http://myserver:8080/tfs", form.getServerName()); //verify getter trims the server name/url

        form.setServerUrl(null);
        assertEquals("", form.getServerName());
    }

    @Test
    public void vsoLoginForm() {
        final VsoLoginForm form = new VsoLoginForm();
        form.setAuthenticating(true);
        assertTrue(form.getBusySpinner().isVisible());
        form.setAuthenticating(false);
        assertFalse(form.getBusySpinner().isVisible());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_USER_ACCOUNT_PANEL_VSO_SERVER_NAME), form.getServerName());
    }

}
