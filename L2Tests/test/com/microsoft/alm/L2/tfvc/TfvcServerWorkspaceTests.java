// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.utils.TfvcCheckoutResultUtils;
import com.microsoft.alm.plugin.idea.common.ui.checkout.VsoCheckoutPageModel;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

public class TfvcServerWorkspaceTests extends TfvcCheckoutTestBase {

    @Override
    protected void setUpCheckoutModel(VsoCheckoutPageModel model, String path) {
        super.setUpCheckoutModel(model, path);
        model.setTfvcServerCheckout(true);
    }

    private void checkoutFile(File file) {
        ServerContext serverContext = ServerContextManager.getInstance().get(getServerUrl());
        TfvcCheckoutResult checkoutResult = CommandUtils.checkoutFilesForEdit(
                serverContext,
                Collections.singletonList(file.toPath()),
                false);
        try {
            TfvcCheckoutResultUtils.verify(checkoutResult);
        } catch (VcsException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 60000)
    public void testCheckout_VSO() throws InterruptedException, IOException {
        checkoutTestRepository(workspace -> {
            File readmeIoFile = workspace.resolve(README_FILE).toFile();
            assertTrue(readmeIoFile.exists());
            VirtualFile readme = Objects.requireNonNull(LocalFileSystem.getInstance().findFileByIoFile(readmeIoFile));
            assertFalse(readme.isWritable());
            checkoutFile(readmeIoFile);
            readme.refresh(false, false);
            assertTrue(readme.isWritable());
        });
    }
}
