// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.microsoft.alm.plugin.services.PropertyService;
import org.junit.Test;

import java.io.IOException;

public class TfvcRootCheckerNormalCheckoutTest extends TfvcRootCheckerTestBase {

    @Test(timeout = 60000)
    public void testRootChecker() throws InterruptedException, IOException {
        PropertyService.getInstance().setUseReactiveClient(false);
        doRootCheckerTest();
        PropertyService.getInstance().setUseReactiveClient(true);
        doRootCheckerTest();
    }
}
