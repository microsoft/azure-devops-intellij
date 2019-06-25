// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.workspace;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.ValidationListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class WorkspaceControllerTest extends IdeaAbstractTest {
    @Mock
    public Project mockProject;
    @Mock
    public WorkspaceDialog mockDialog;
    @Mock
    public WorkspaceModel mockModel;

    final String name = "name1";
    final String server = "server1";
    final String comment = "this is a comment";
    final String computer = "computer1";
    final List<Workspace.Mapping> mappings = new ArrayList<Workspace.Mapping>();
    final String owner = "owner1";
    final boolean isLoading = true;

    @Before
    public void mockOtherObjects() {
        MockitoAnnotations.initMocks(this);

        when(mockDialog.getWorkingFolders()).thenReturn(mappings);
        when(mockDialog.getWorkspaceComment()).thenReturn(comment);
        when(mockDialog.getWorkspaceName()).thenReturn(name);

        when(mockModel.getName()).thenReturn(name);
        when(mockModel.getServer()).thenReturn(server);
        when(mockModel.getComment()).thenReturn(comment);
        when(mockModel.getComputer()).thenReturn(computer);
        when(mockModel.getMappings()).thenReturn(mappings);
        when(mockModel.getOwner()).thenReturn(owner);
        when(mockModel.getLocation()).thenReturn(Workspace.Location.SERVER);
        when(mockModel.isLoading()).thenReturn(isLoading);
    }

    /**
     * This test will make sure that the controller ends up in the correct default state
     * after being constructed and that the model and dialog are in sync.
     */
    @Test
    public void testConstructor() {
        final WorkspaceController c = new WorkspaceController(mockProject, mockDialog, mockModel);

        // Check that the dialog and model were connected correctly to the controller
        Mockito.verify(mockDialog).addActionListener(c);
        Mockito.verify(mockDialog).addValidationListener(Matchers.any(ValidationListener.class));
        Mockito.verify(mockModel).addObserver(c);
    }

    @Test
    public void testUpdate_null() {
        final WorkspaceController c = new WorkspaceController(mockProject, mockDialog, mockModel);

        // When we call update with null for the argument/property
        // it should update all properties except isLoading
        c.update(mockModel, null);
        Mockito.verify(mockDialog).setName(name);
        Mockito.verify(mockDialog).setServer(server);
        Mockito.verify(mockDialog).setComment(comment);
        Mockito.verify(mockDialog).setComputer(computer);
        Mockito.verify(mockDialog).setMappings(mappings);
        Mockito.verify(mockDialog).setOwner(owner);
        Mockito.verify(mockDialog, times(0)).setLoading(anyBoolean());
    }

    @Test
    public void testUpdate_comment() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_COMMENT);
        Mockito.verify(mockDialog).setComment(comment);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_computer() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_COMPUTER);
        Mockito.verify(mockDialog).setComputer(computer);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_loading() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_LOADING);
        Mockito.verify(mockDialog).setLoading(isLoading);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_mappings() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_MAPPINGS);
        Mockito.verify(mockDialog).setMappings(mappings);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_name() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_NAME);
        Mockito.verify(mockDialog).setName(name);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_owner() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_OWNER);
        Mockito.verify(mockDialog).setOwner(owner);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_server() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_SERVER);
        Mockito.verify(mockDialog).setServer(server);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_location() {
        final WorkspaceController c = getController();
        c.update(mockModel, WorkspaceModel.PROP_LOCATION);
        Mockito.verify(mockDialog).setLocation(Workspace.Location.SERVER);
        Mockito.verifyNoMoreInteractions(mockDialog);
    }

    private WorkspaceController getController() {
        final WorkspaceController c = new WorkspaceController(mockProject, mockDialog, mockModel);
        Mockito.verify(mockDialog).addActionListener(c);
        Mockito.verify(mockDialog).addValidationListener(Matchers.any(ValidationListener.class));
        return c;
    }

    @Test
    public void testValidate_noErrors() {
        final WorkspaceController c = getController();
        when(mockModel.validate()).thenReturn(null);
        when(mockDialog.getFirstMappingValidationError()).thenReturn(null);

        Assert.assertEquals(null, c.validate());
    }

    @Test
    public void testValidate_modelError() {
        final WorkspaceController c = getController();
        ModelValidationInfo error = ModelValidationInfo.createWithMessage("error1");
        when(mockModel.validate()).thenReturn(error);
        when(mockDialog.getFirstMappingValidationError()).thenReturn(null);

        Assert.assertEquals(error.getValidationMessage(), c.validate().message);
    }

    @Test
    public void testValidate_dialogError() {
        final WorkspaceController c = getController();
        when(mockModel.validate()).thenReturn(null);
        when(mockDialog.getFirstMappingValidationError()).thenReturn("error1");

        Assert.assertEquals("error1", c.validate().message);
    }


    @Test
    public void testValidate_bothErrors() {
        final WorkspaceController c = getController();
        ModelValidationInfo error = ModelValidationInfo.createWithMessage("error1");
        when(mockModel.validate()).thenReturn(error);
        when(mockDialog.getFirstMappingValidationError()).thenReturn("error2");

        // Make sure that the model error takes precedence
        Assert.assertEquals("error1", c.validate().message);
    }


    /**
     * This test makes sure that the Controller correctly updates the model based on events coming from the UI
     */
    @Test
    public void testDialogBinding() {
        final WorkspaceController c = new WorkspaceController(mockProject, mockDialog, mockModel);

        // When we call update with null for the argument/property
        // it should update all properties except isLoading
        c.updateModel();
        Mockito.verify(mockModel).setName(name);
        Mockito.verify(mockModel).setComment(comment);
        Mockito.verify(mockModel).setMappings(mappings);
    }
}
