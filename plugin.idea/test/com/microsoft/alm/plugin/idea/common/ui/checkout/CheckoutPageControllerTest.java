// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.checkout.mocks.MockCheckoutPage;
import com.microsoft.alm.plugin.idea.common.ui.checkout.mocks.MockCheckoutPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.idea.common.ui.controls.UserAccountPanel;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckoutPageControllerTest extends IdeaAbstractTest {
    /**
     * This test was added to cover the bit of the tfs controller that wasn't already covered
     */
    @Test
    public void testActionPerformed() {
        MockCheckoutPageModel tfsModel = new MockCheckoutPageModel(null, ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        MockCheckoutPage tfsPage = new MockCheckoutPage();
        CheckoutPageController tcc = new CheckoutPageController(null, tfsModel, tfsPage);

        // Trigger the controller to do connect
        tcc.actionPerformed(new ActionEvent(this, 1, LoginForm.CMD_SIGN_IN));
        assertEquals(true, tfsModel.isLoadRepositoriesCalled());
        tfsModel.clearInternals();

        // Trigger the controller to do sign out
        assertEquals(true, tfsModel.isConnected());
        tcc.actionPerformed(new ActionEvent(this, 1, UserAccountPanel.CMD_SIGN_OUT));
        assertEquals(false, tfsModel.isConnected());
        tfsModel.clearInternals();

        // Trigger the controller to do filter changed
        assertTrue(StringUtils.isEmpty(tfsModel.getRepositoryFilter()));
        tfsPage.setRepositoryFilter("filter");
        tcc.actionPerformed(new ActionEvent(this, 1, CheckoutForm.CMD_REPO_FILTER_CHANGED));
        assertEquals("filter", tfsModel.getRepositoryFilter());
        tfsModel.clearInternals();
    }

}
