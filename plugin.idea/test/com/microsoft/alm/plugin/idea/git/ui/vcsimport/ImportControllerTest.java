// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks.MockImportPage;
import com.microsoft.alm.plugin.idea.git.ui.checkout.mocks.MockBaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks.MockImportModel;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks.MockImportPageModel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;

public class ImportControllerTest extends IdeaAbstractTest {

    MockBaseDialog dialog;
    MockImportModel model;
    ImportController importController;

    @Before
    public void initialize() {
        dialog = new MockBaseDialog();
        final MockImportPage vsoPage = new MockImportPage();
        final MockImportPage tfsPage = new MockImportPage();
        model = new MockImportModel();
        importController = new ImportController(dialog, vsoPage, tfsPage, model);
    }

    /**
     * This test verifies the import controller is initialize correctly with the default state,
     * and that the model and dialog are in sync
     */
    @Test
    public void testConstructor() {
        //Verify VSO tab is selected by default
        Assert.assertEquals("VSO tab was not selected on the import dialog",
                ImportController.TAB_VSO, dialog.getSelectedTabIndex());
        Assert.assertTrue("VSO is not selected on the import model",
                model.isVsoSelected());

        //Verify listeners are hooked up
        Assert.assertEquals("Action Listeners count not as expected",
                1, dialog.getActionListeners().size());
        Assert.assertEquals("Validation Listeners count not as expected",
                1, dialog.getValidationListeners().size());
    }

    /**
     * This test verifies the update method is hooke up correctly to changes in the Import model.
     * It verifies the UI is updated via the controller correctly when the model is changed.
     */
    @Test
    public void testUpdate() {
        //Verify Selecting VSO and TFS tab works as expected
        Assert.assertEquals("VSO tab was not selected initially on the import dialog",
                ImportController.TAB_VSO, dialog.getSelectedTabIndex());
        importController.getModel().setVsoSelected(false);
        Assert.assertEquals("Import dialog didn't switch to TFS tab when model was updated",
                ImportController.TAB_TFS, dialog.getSelectedTabIndex());
        importController.getModel().setVsoSelected(true);
        Assert.assertEquals("Import dialog didn't switch to VSO tab when model was updated",
                ImportController.TAB_VSO, dialog.getSelectedTabIndex());

        // Verify ImportEnabled behavior is properly connected through controller for Vso
        importController.getModel().setVsoSelected(true);
        Assert.assertEquals(false, dialog.isOkEnabled());
        importController.getModel().getVsoImportPageModel().setConnected(true);
        importController.getModel().updateImportEnabled();
        Assert.assertEquals(true, dialog.isOkEnabled());
        importController.getModel().getVsoImportPageModel().setConnected(false);
        importController.getModel().updateImportEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        // Verify ImportEnabled behavior is properly connected through controller for Tfs
        importController.getModel().setVsoSelected(false);
        Assert.assertEquals(false, dialog.isOkEnabled());
        importController.getModel().getTfsImportPageModel().setConnected(true);
        importController.getModel().updateImportEnabled();
        Assert.assertEquals(true, dialog.isOkEnabled());
        importController.getModel().getTfsImportPageModel().setConnected(false);
        importController.getModel().updateImportEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        // Verify ImportEnabled is enabled/disabled only when we are on the right page
        importController.getModel().setVsoSelected(true);
        Assert.assertEquals(false, dialog.isOkEnabled());
        importController.getModel().getTfsImportPageModel().setConnected(true);
        importController.getModel().updateImportEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());

        importController.getModel().getTfsImportPageModel().setConnected(false);
        importController.getModel().getVsoImportPageModel().setConnected(true);
        importController.getModel().setVsoSelected(false);
        importController.getModel().updateImportEnabled();
        Assert.assertEquals(false, dialog.isOkEnabled());


        //Verify errors show up correctly on dialog when model has errors
        Assert.assertNull("Errors were shown on Import dialog when there were no errors",
                dialog.getDisplayError());
        importController.getModel().addError(ModelValidationInfo.createWithMessage("error 1"));
        Assert.assertEquals("Error on import dialog does not match the first error on the model",
                "error 1", dialog.getDisplayError());
        importController.getModel().addError(ModelValidationInfo.createWithMessage("another error"));
        Assert.assertEquals("Error on import dialog does not match the first error on the model",
                "error 1", dialog.getDisplayError());

    }

    /**
     * This test verifies that the controller updates the model correctly based on events from the UI (ImportDialog)
     */
    @Test
    public void testDialogBinding() {

        //Verify tab selection on dialog updates model
        dialog.setSelectedTabIndex(ImportController.TAB_TFS);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        Assert.assertEquals("Import dialog did not switch to TFS tab",
                ImportController.TAB_TFS, dialog.getSelectedTabIndex());
        Assert.assertFalse("Model was not updated when TFS tab was selected on Import dialog",
                importController.getModel().isVsoSelected());

        dialog.setSelectedTabIndex(ImportController.TAB_VSO);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        Assert.assertEquals("Import dialog did not switch to VSO tab",
                ImportController.TAB_VSO, dialog.getSelectedTabIndex());
        Assert.assertTrue("Model was not updated when VSO tab was selected on Import dialog",
                importController.getModel().isVsoSelected());

        //Verify clicking the Import button works on both tabs
        dialog.setSelectedTabIndex(ImportController.TAB_VSO);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_OK));
        Assert.assertTrue("Import was not called when button was pressed in the dialog on VSO tab",
                model.getMockVsoImportPageModel().isImportCalled());
        model.getMockVsoImportPageModel().clearInternals();
        dialog.setSelectedTabIndex(ImportController.TAB_TFS);
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_TAB_CHANGED));
        dialog.getActionListeners().get(0).actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_OK));
        Assert.assertTrue("Import was not called when button was pressed in the dialog on TFS tab",
                model.getMockTfsImportPageModel().isImportCalled());
        model.getMockTfsImportPageModel().clearInternals();
    }

    /**
     * This test makes sure the errors on the model are given priority over form validation errors
     */
    @Test
    public void testValidate() {
        Assert.assertNull("Unexpected errros on import dialog when first opened",
                dialog.getDisplayError());

        model.getMockVsoImportPageModel().setValidationError("page model error");
        dialog.validate();
        Assert.assertEquals("Error on dialog not as expected",
                "page model error", dialog.getDisplayError());

        //Add a new error on the model, verify errors from model take precedence over page/form validation errors
        model.addError(ModelValidationInfo.createWithMessage("import model error"));
        dialog.validate();
        Assert.assertEquals("Error on dialog not as expected when there are multiple errors",
                "import model error", dialog.getDisplayError());

        //clear errors on import model
        model.clearErrors();
        dialog.validate();
        Assert.assertEquals("Error on dialog not as expected",
                "page model error", dialog.getDisplayError());

        //clear validation errors on page model
        model.getMockVsoImportPageModel().setValidationError(MockImportPageModel.NO_ERRORS);
        dialog.validate();
        Assert.assertNull("Unexpected errros on import dialog when validation errors and model errors are cleared",
                dialog.getDisplayError());

        //verify errors on model work by themselves
        model.addError(ModelValidationInfo.createWithMessage("import model connection error"));
        dialog.validate();
        Assert.assertEquals("Expected error not found on import dialog",
                "import model connection error", dialog.getDisplayError());
    }
}
