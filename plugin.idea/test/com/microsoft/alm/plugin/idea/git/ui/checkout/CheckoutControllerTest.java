// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.checkout;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.git.ui.checkout.mocks.MockBaseDialog;
import com.microsoft.alm.plugin.idea.git.ui.checkout.mocks.MockCheckoutModel;
import com.microsoft.alm.plugin.idea.git.ui.checkout.mocks.MockCheckoutPage;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import org.junit.Assert;
import org.junit.Test;

import java.awt.event.ActionEvent;

public class CheckoutControllerTest extends IdeaAbstractTest {

    /**
     * This test will make sure that the controller ends up in the correct default state
     * after being constructed and that the model and dialog are in sync.
     */
    @Test
    public void testConstructor() {
        MockBaseDialog dialog = new MockBaseDialog();
        MockCheckoutPage vsoPage = new MockCheckoutPage();
        MockCheckoutPage tfsPage = new MockCheckoutPage();
        MockCheckoutModel model = new MockCheckoutModel();
        CheckoutController cc = new CheckoutController(dialog, vsoPage, tfsPage, model);

        // Ensure that clone starts off disabled and the VSO tab is selected
        Assert.assertEquals(false, dialog.isOkEnabled());
        Assert.assertEquals(false, cc.getModel().isCloneEnabled());
        Assert.assertEquals(CheckoutController.TAB_VSO, dialog.getSelectedTabIndex());
        Assert.assertEquals(true, cc.getModel().isVsoSelected());

        // Make sure the listeners got hooked up
        Assert.assertEquals(dialog.getActionListeners().size(), 1);
    }

    /**
     * This test makes sure that the update method is properly hooked up by the Controller onto the Model
     * We change the model and make sure the UI is updated via the controller.
     */
    @Test
    public void testUpdate() {
        MockBaseDialog dialog = new MockBaseDialog();
        MockCheckoutPage vsoPage = new MockCheckoutPage();
        MockCheckoutPage tfsPage = new MockCheckoutPage();
        MockCheckoutModel model = new MockCheckoutModel();
        CheckoutController cc = new CheckoutController(dialog, vsoPage, tfsPage, model);

        // Verify CloneEnabled behavior is properly connected through controller for Vso
        cc.getModel().setVsoSelected(true);
        Assert.assertEquals(false, dialog.isOkEnabled());
        cc.getModel().getVsoModel().setConnected(true);
        cc.getModel().updateCloneEnabled();
        Assert.assertEquals(true, dialog.isOkEnabled());
        cc.getModel().getVsoModel().setConnected(false);
        cc.getModel().updateCloneEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        // Verify CloneEnabled behavior is properly connected through controller for Tfs
        cc.getModel().setVsoSelected(false);
        Assert.assertEquals(false, dialog.isOkEnabled());
        cc.getModel().getTfsModel().setConnected(true);
        cc.getModel().updateCloneEnabled();
        Assert.assertEquals(true, dialog.isOkEnabled());
        cc.getModel().getTfsModel().setConnected(false);
        cc.getModel().updateCloneEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        // Verify CloneEnabled is enabled/disabled only when we are on the right page
        cc.getModel().setVsoSelected(true);
        Assert.assertEquals(false, dialog.isOkEnabled());
        cc.getModel().getTfsModel().setConnected(true);
        cc.getModel().updateCloneEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        cc.getModel().getTfsModel().setConnected(false);
        cc.getModel().getVsoModel().setConnected(true);
        cc.getModel().setVsoSelected(false);
        cc.getModel().updateCloneEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        // Verify SelectedTabIndex behavior is properly connected thru controller
        cc.getModel().setVsoSelected(true);
        Assert.assertEquals(CheckoutController.TAB_VSO, dialog.getSelectedTabIndex());
        cc.getModel().setVsoSelected(false);
        Assert.assertEquals(CheckoutController.TAB_TFS, dialog.getSelectedTabIndex());
        cc.getModel().setVsoSelected(true);
        Assert.assertEquals(CheckoutController.TAB_VSO, dialog.getSelectedTabIndex());

        // Verify SelectedTabIndex behavior is properly connected thru controller
        Assert.assertEquals(null, dialog.getDisplayError());
        cc.getModel().addError(ModelValidationInfo.createWithMessage("error1"));
        Assert.assertEquals("error1", dialog.getDisplayError());
        cc.getModel().addError(ModelValidationInfo.createWithMessage("error2"));
        // The first error should still be returned
        Assert.assertEquals("error1", dialog.getDisplayError());
        cc.getModel().clearErrors();
        Assert.assertEquals(null, dialog.getDisplayError());
    }

    /**
     * This test makes sure that the Controller correctly updates the model based on events coming from the UI
     */
    @Test
    public void testDialogBinding() {
        MockBaseDialog dialog = new MockBaseDialog();
        MockCheckoutPage vsoPage = new MockCheckoutPage();
        MockCheckoutPage tfsPage = new MockCheckoutPage();
        MockCheckoutModel model = new MockCheckoutModel();
        CheckoutController cc = new CheckoutController(dialog, vsoPage, tfsPage, model);

        // Verify SelectedTabIndex behavior is properly connected thru controller
        Assert.assertEquals(CheckoutController.TAB_VSO, dialog.getSelectedTabIndex());
        Assert.assertEquals(true, cc.getModel().isVsoSelected());
        dialog.setSelectedTabIndex(CheckoutController.TAB_TFS);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        Assert.assertEquals(CheckoutController.TAB_TFS, dialog.getSelectedTabIndex());
        Assert.assertEquals(false, cc.getModel().isVsoSelected());
        dialog.setSelectedTabIndex(CheckoutController.TAB_VSO);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        Assert.assertEquals(CheckoutController.TAB_VSO, dialog.getSelectedTabIndex());
        Assert.assertEquals(true, cc.getModel().isVsoSelected());

        // Verify the Clone button click for both tabs
        Assert.assertEquals(false, model.getMockVsoModel().isCloneSelectedRepoCalled());
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_OK));
        Assert.assertEquals(true, model.getMockVsoModel().isCloneSelectedRepoCalled());
        model.getMockVsoModel().clearInternals();
        dialog.setSelectedTabIndex(CheckoutController.TAB_TFS);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        Assert.assertEquals(false, model.getMockTfsModel().isCloneSelectedRepoCalled());
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_OK));
        Assert.assertEquals(true, model.getMockTfsModel().isCloneSelectedRepoCalled());
        model.getMockTfsModel().clearInternals();
    }

    /**
     * This test makes sure that the validate method prefers errors from the list to validation errors.
     */
    @Test
    public void testValidate() {
        MockBaseDialog dialog = new MockBaseDialog();
        MockCheckoutPage vsoPage = new MockCheckoutPage();
        MockCheckoutPage tfsPage = new MockCheckoutPage();
        MockCheckoutModel model = new MockCheckoutModel();
        CheckoutController cc = new CheckoutController(dialog, vsoPage, tfsPage, model);

        Assert.assertEquals(null, dialog.getDisplayError());

        // Add a validate error
        model.getMockVsoModel().setValidationError("validationError1");
        dialog.validate();
        Assert.assertEquals("validationError1", dialog.getDisplayError());

        // Now add an error to the list and make sure it is preferred
        model.addError(ModelValidationInfo.createWithMessage("error1"));
        dialog.validate();
        Assert.assertEquals("error1", dialog.getDisplayError());

        // Now clear the errors
        model.clearErrors();
        dialog.validate();
        Assert.assertEquals("validationError1", dialog.getDisplayError());

        // Now clear the validation error
        model.getMockVsoModel().setValidationError("");
        dialog.validate();
        Assert.assertEquals(null, dialog.getDisplayError());

        // Finally, make sure that errors work by themselves
        model.addError(ModelValidationInfo.createWithMessage("error2"));
        dialog.validate();
        Assert.assertEquals("error2", dialog.getDisplayError());

    }

}