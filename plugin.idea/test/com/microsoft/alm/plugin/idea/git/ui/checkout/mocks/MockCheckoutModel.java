// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.checkout.mocks;

import com.microsoft.alm.plugin.idea.git.ui.checkout.CheckoutModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;

public class MockCheckoutModel extends CheckoutModel {
    public MockCheckoutModel() {
        super(null, null, new MockCheckoutPageModel(null, ServerContextTableModel.VSO_REPO_COLUMNS),
                new MockCheckoutPageModel(null, ServerContextTableModel.TFS_REPO_COLUMNS));
        ((MockCheckoutPageModel) getVsoModel()).initialize(this);
        ((MockCheckoutPageModel) getTfsModel()).initialize(this);
    }

    public MockCheckoutPageModel getMockVsoModel() {
        return (MockCheckoutPageModel) getVsoModel();
    }

    public MockCheckoutPageModel getMockTfsModel() {
        return (MockCheckoutPageModel) getTfsModel();
    }
}
