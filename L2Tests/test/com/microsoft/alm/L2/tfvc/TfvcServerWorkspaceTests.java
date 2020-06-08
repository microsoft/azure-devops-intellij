// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.utils.TfvcCheckoutResultUtils;
import com.microsoft.alm.plugin.idea.common.ui.checkout.VsoCheckoutPageModel;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

public class TfvcServerWorkspaceTests extends TfvcCheckoutTestBase {

    @Override
    protected void setUpCheckoutModel(VsoCheckoutPageModel model, String path) {
        super.setUpCheckoutModel(model, path);
        model.setTfvcServerCheckout(true);
    }

    @NotNull
    private ServerContext getServerContext() {
        return ServerContextManager.getInstance().get(getServerUrl());
    }

    private void checkoutFile(File file) {
        TfvcCheckoutResult checkoutResult = CommandUtils.checkoutFilesForEdit(
                getServerContext(),
                Collections.singletonList(file.toPath()),
                false);
        try {
            TfvcCheckoutResultUtils.verify(checkoutResult);
        } catch (VcsException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertIsServerWorkspace(Path workspace) {
        Workspace partialWorkspace = CommandUtils.getPartialWorkspace(workspace, false);
        AuthenticationInfo authenticationInfo = getServerContext().getAuthenticationInfo();
        Workspace workspaceInfo = CommandUtils.getDetailedWorkspace(
                partialWorkspace.getServerDisplayName(),
                partialWorkspace.getName(),
                authenticationInfo);
        assertEquals(Workspace.Location.SERVER, workspaceInfo.getLocation());
    }

    @Test(timeout = 60000)
    public void testServerCheckout() throws InterruptedException, IOException {
        checkoutTestRepository(this::assertIsServerWorkspace);
    }
}
