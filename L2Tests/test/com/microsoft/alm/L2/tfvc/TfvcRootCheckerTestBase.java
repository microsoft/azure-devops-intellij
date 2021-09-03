// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.intellij.openapi.vcs.VcsRootChecker;
import com.microsoft.alm.plugin.idea.tfvc.extensions.TfvcRootChecker;
import com.microsoft.alm.plugin.services.PropertyService;

import java.io.File;
import java.io.IOException;

public abstract class TfvcRootCheckerTestBase extends TfvcCheckoutTestBase {

    private boolean savedUseReactiveClientValue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        savedUseReactiveClientValue = PropertyService.getInstance().useReactiveClient();
    }

    @Override
    protected void tearDown() throws Exception {
        PropertyService.getInstance().setUseReactiveClient(savedUseReactiveClientValue);
        super.tearDown();
    }

    protected void doRootCheckerTest() throws IOException, InterruptedException {
        checkoutTestRepository(workspace -> {
            TfvcRootChecker rootChecker = VcsRootChecker.EXTENSION_POINT_NAME.findExtension(TfvcRootChecker.class);
            assertNotNull(rootChecker);
            assertTrue(rootChecker.isRoot(workspace.toString()));

            File tempDirectory;
            try {
                tempDirectory = createTempDirectory();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertFalse(rootChecker.isRoot(tempDirectory.getAbsolutePath()));
        });
    }
}
