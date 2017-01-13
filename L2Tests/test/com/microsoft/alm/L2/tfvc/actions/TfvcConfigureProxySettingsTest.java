// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc.actions;

import com.microsoft.alm.L2.L2Test;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import com.microsoft.alm.plugin.idea.tfvc.actions.ConfigureProxyAction;
import com.microsoft.alm.plugin.idea.tfvc.ui.ProxySettingsDialog;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({ConfigureProxyAction.class})
public class TfvcConfigureProxySettingsTest extends L2Test {

    @Test(timeout = 60000)
    public void testConfigureProxyAction_update() throws Exception {
        final String serverUri = "http://server:8080/tfs/collection1";
        mockRepositoryContextForProject(serverUri);
        final ProxySettingsDialog dialog = Mockito.mock(ProxySettingsDialog.class);
        PowerMockito.whenNew(ProxySettingsDialog.class).withAnyArguments().thenReturn(dialog);
        Mockito.when(dialog.showAndGet()).thenReturn(true);
        Mockito.when(dialog.getProxyUri()).thenReturn("http://proxy:8888");

        // Make sure proxy is not set for the server
        Assert.assertNull(WorkspaceHelper.getProxyServer(serverUri));

        // Run the action and verify that the proxy was set
        final ConfigureProxyAction action = new ConfigureProxyAction();
        myCodeInsightFixture.testAction(action);
        Mockito.verify(dialog).getProxyUri();
        Assert.assertEquals("http://proxy:8888", WorkspaceHelper.getProxyServer(serverUri));

        // Remove proxy for any other tests
        WorkspaceHelper.setProxyServer(serverUri, null);
    }
}