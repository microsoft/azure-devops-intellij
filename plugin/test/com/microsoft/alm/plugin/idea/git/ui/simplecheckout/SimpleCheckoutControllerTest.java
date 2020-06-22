// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.simplecheckout;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SimpleCheckoutControllerTest extends IdeaAbstractTest {
    public static final String GIT_URL = "https://organization.visualstudio.com/DefaultCollection/_git/TestProject";
    public static final String DIRECTORY_NAME = "directoryName";
    public static final String PARENT_DIRECTORY = "/parent/directory";

    @Mock
    public SimpleCheckoutDialog mockDialog;
    @Mock
    public SimpleCheckoutModel mockModel;

    public SimpleCheckoutController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockModel.getRepoUrl()).thenReturn(GIT_URL);
        when(mockModel.getDirectoryName()).thenReturn(DIRECTORY_NAME);
        when(mockModel.getParentDirectory()).thenReturn(PARENT_DIRECTORY);
        controller = new SimpleCheckoutController(mockDialog, mockModel);
    }

    @Test
    public void testConstructor() {
        verify(mockDialog).setRepoUrl(mockModel.getRepoUrl());
        verify(mockDialog).setDirectoryName(mockModel.getDirectoryName());
        verify(mockDialog).setParentDirectory(mockModel.getParentDirectory());
    }

    @Test
    public void testActionPerformed() {
        ActionEvent e = new ActionEvent(this, 1, BaseDialog.CMD_OK);
        controller.actionPerformed(e);

        verify(mockModel).cloneRepo();
    }

    @Test
    public void testUpdateDirectoryName() {
        controller.update(null, SimpleCheckoutModel.PROP_DIRECTORY_NAME);

        // called twice, when controller is created and then when update is called
        verify(mockDialog, times(2)).setDirectoryName(mockModel.getDirectoryName());
        // called once, when controller is created
        verify(mockDialog).setParentDirectory(mockModel.getParentDirectory());
    }

    @Test
    public void testUpdateParentDirectory() {
        controller.update(null, SimpleCheckoutModel.PROP_PARENT_DIR);

        // called twice, when controller is created and then when update is called
        verify(mockDialog, times(2)).setParentDirectory(mockModel.getParentDirectory());
        // called once, when controller is created
        verify(mockDialog).setDirectoryName(mockModel.getDirectoryName());
    }

    @Test
    public void testValidate_Happy() {
        when(mockModel.validate()).thenReturn(ModelValidationInfo.NO_ERRORS);
        Assert.assertNull(controller.validate());
    }

    @Test
    public void testValidate_Error() {
        String messageKey = TfPluginBundle.KEY_VCS_LOADING_ERRORS;
        String message = TfPluginBundle.message(messageKey);
        String componentName = "component";
        JComponent mockComponent = mock(JComponent.class);

        when(mockDialog.getComponent(componentName)).thenReturn(mockComponent);
        ValidationInfo expectInfo = new ValidationInfo(message, mockComponent);
        when(mockModel.validate()).thenReturn(ModelValidationInfo.createWithResource(componentName, messageKey));
        ValidationInfo actualInfo = controller.validate();

        Assert.assertEquals(expectInfo.message, actualInfo.message);
        Assert.assertEquals(expectInfo.component, actualInfo.component);
    }
}
