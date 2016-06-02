// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.branch;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import git4idea.GitRemoteBranch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.ComboBoxModel;
import java.awt.event.ActionEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateBranchControllerTest extends IdeaAbstractTest {

    private CreateBranchController underTest;

    @Mock
    private CreateBranchDialog mockDialog;
    @Mock
    private CreateBranchModel mockModel;

    @Before
    public void setUp() {
        underTest = new CreateBranchController(mockDialog, mockModel);
        reset(mockDialog);
        reset(mockModel);
    }

    @Test
    public void testActionPerformed() throws Exception {
        ActionEvent e = new ActionEvent(this, 1, BaseDialog.CMD_OK);
        underTest.actionPerformed(e);

        verify(mockModel).setBranchName(any(String.class));
        verify(mockModel).setSelectedRemoteBranch(any(GitRemoteBranch.class));
    }

    @Test
    public void testUpdate_BranchName() {
        underTest.update(null, CreateBranchModel.PROP_BRANCH_NAME);
        verify(mockDialog).setBranchName(any(String.class));
    }

    @Test
    public void testUpdate_ComboBox() {
        underTest.update(null, CreateBranchModel.PROP_REMOTE_BRANCH_COMBO_MODEL);
        verify(mockDialog).setRemoteBranchDropdownModel(any(ComboBoxModel.class));
    }

    @Test
    public void testUpdate_SelectedRemote() {
        underTest.update(null, CreateBranchModel.PROP_SELECTED_REMOTE_BRANCH);
        verify(mockDialog).setSelectedRemoteBranch(any(GitRemoteBranch.class));
    }

    @Test
    public void testValidate_NoErrors() {
        when(mockModel.validate()).thenReturn(ModelValidationInfo.NO_ERRORS);
        Assert.assertNull(underTest.validate());
    }

    @Test
    public void testValidate_Errors() {
        ModelValidationInfo mockValidationInfo = mock(ModelValidationInfo.class);
        when(mockValidationInfo.getValidationMessage()).thenReturn(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_ERRORS_BRANCH_NAME_EMPTY);
        when(mockValidationInfo.getValidationSource()).thenReturn(CreateBranchModel.PROP_BRANCH_NAME);
        when(mockModel.validate()).thenReturn(mockValidationInfo);

        ValidationInfo validationInfo = underTest.validate();
        Assert.assertEquals(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_ERRORS_BRANCH_NAME_EMPTY, validationInfo.message);
    }

    @Test
    public void testUpdateModel() throws Exception {
        underTest.updateModel();
        verify(mockModel).setBranchName(any(String.class));
        verify(mockModel).setSelectedRemoteBranch(any(GitRemoteBranch.class));
    }
}
