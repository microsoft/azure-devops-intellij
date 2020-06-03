// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TfvcCheckoutTest extends TfvcCheckoutTestBase {

    @Test(timeout = 60000)
    public void testCheckout_VSO() throws InterruptedException, IOException {
        checkoutTestRepository(workspace -> {
            // verify that the readme was downloaded
            File readme = workspace.resolve(README_FILE).toFile();
            Assert.assertTrue(readme.exists());
        });
    }
}
