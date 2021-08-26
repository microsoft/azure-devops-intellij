// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.microsoft.alm.plugin.idea.common.ui.checkout.VsoCheckoutPageModel;
import com.microsoft.alm.plugin.services.PropertyService;
import org.junit.Test;

import java.io.IOException;

public class TfvcRootCheckerServerCheckoutTest extends TfvcRootCheckerTestBase {

    @Override
    protected void setUpCheckoutModel(VsoCheckoutPageModel model, String path) {
        super.setUpCheckoutModel(model, path);
        model.setTfvcServerCheckout(true);
    }

    @Test(timeout = 60000)
    public void testRootChecker() throws InterruptedException, IOException {
        PropertyService.getInstance().setProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT, "false");
        doRootCheckerTest();
        PropertyService.getInstance().setProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT, "true");
        doRootCheckerTest();
    }
}
