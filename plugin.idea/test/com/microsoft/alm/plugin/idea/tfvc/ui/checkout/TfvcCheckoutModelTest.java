// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.checkout;

import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockServerContext;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class TfvcCheckoutModelTest extends IdeaAbstractTest {
    /**
     * This is just a really basic test of the constructor
     * It checks the values of the properties right after construction
     */
    @Test
    public void testConstructor() {
        final TfvcCheckoutModel model = new TfvcCheckoutModel();

        // Make sure default values are set correctly
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_CREATE_WORKSPACE_BUTTON),
                model.getButtonText());
        Assert.assertEquals("", model.getRepositoryName(null));
        Assert.assertEquals(RepositoryContext.Type.TFVC, model.getRepositoryType());
    }

    @Test
    public void testGetRepositoryName() {
        final ServerContext context = getServerContext("https://test.visualstudio.com", "collection1", "project1");
        final TfvcCheckoutModel model = new TfvcCheckoutModel();
        Assert.assertEquals("project1", model.getRepositoryName(context));
    }

    @Test
    public void testGetRepositoryName_nullContext() {
        final TfvcCheckoutModel model = new TfvcCheckoutModel();
        Assert.assertEquals("", model.getRepositoryName(null));
    }

    @Test
    public void testGetRepositoryName_nullProject() {
        final ServerContext context = getServerContext("https://test.visualstudio.com", "collection1", null);
        final TfvcCheckoutModel model = new TfvcCheckoutModel();
        Assert.assertEquals("", model.getRepositoryName(context));
    }

    private ServerContext getServerContext(String serverName, String collectionName, String projectName) {
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        collection.setName(collectionName);
        TeamProjectReference teamProject1 = null;
        if (StringUtils.isNotEmpty(projectName)) {
            teamProject1 = new TeamProjectReference();
            teamProject1.setName(projectName);
        }
        return new MockServerContext(ServerContext.Type.TFS, null, URI.create(serverName), collection, teamProject1, null);
    }
}
