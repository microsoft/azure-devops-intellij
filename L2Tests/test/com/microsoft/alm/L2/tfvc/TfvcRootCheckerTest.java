// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.intellij.openapi.vcs.VcsRootChecker;
import com.microsoft.alm.plugin.idea.tfvc.extensions.TfvcRootChecker;
import com.microsoft.alm.plugin.services.PropertyService;
import org.junit.Test;

import java.io.IOException;

public class TfvcRootCheckerTest extends TfvcCheckoutTestBase {

    private String savedUseReactiveClientValue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        savedUseReactiveClientValue = PropertyService.getInstance()
                .getProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT);
    }

    @Override
    protected void tearDown() throws Exception {
        PropertyService.getInstance()
                .setProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT, savedUseReactiveClientValue);
        super.tearDown();
    }

    private void doRootCheckerTest() throws IOException, InterruptedException {
        checkoutTestRepository(workspace -> {
            TfvcRootChecker rootChecker = VcsRootChecker.EXTENSION_POINT_NAME.findExtension(TfvcRootChecker.class);
            assertNotNull(rootChecker);
            assertTrue(rootChecker.isRoot(workspace.toString()));
        });
    }

    @Test(timeout = 60000)
    public void testRootChecker() throws InterruptedException, IOException {
        PropertyService.getInstance().setProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT, "false");
        doRootCheckerTest();
        PropertyService.getInstance().setProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT, "true");
        doRootCheckerTest();
    }
}
