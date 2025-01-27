// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.event.ActionEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CreatePullRequestControllerTest extends IdeaAbstractTest {

    CreatePullRequestController underTest;

    CreatePullRequestModel modelMock;

    CreatePullRequestDialog dialogMock;

    @Before
    public void setUp() {
        modelMock = Mockito.mock(CreatePullRequestModel.class);
        dialogMock = Mockito.mock(CreatePullRequestDialog.class);

        underTest = new CreatePullRequestController();
        underTest.setCreateDialog(dialogMock);
        underTest.setCreateModel(modelMock);
    }

    @Test
    public void testActionPerformed() {
        ActionEvent e = new ActionEvent(this, 1, CreatePullRequestForm.CMD_TARGET_BRANCH_UPDATED);
        underTest.actionPerformed(e);

        verify(modelMock).setTargetBranch(any());
        verify(modelMock).loadDiff();
    }

    @Test
    public void whenSourceOfUpdateIsNotSetUpdateAll() {
        underTest.update(null, null);
        verify(dialogMock).setSourceBranch(any());
        verify(dialogMock).setTargetBranchDropdownModel(any());
        verify(dialogMock).setSelectedTargetBranch(any());
        verify(dialogMock).setTitle(any());
        verify(dialogMock).setDescription(any());
        verify(dialogMock).setIsLoading(anyBoolean());
        verify(dialogMock).populateDiff(any(), any());
    }

    /* test some specific updates */
    @Test
    public void whenTitleIsChangedOnlyTitleShouldBeUpdated() {
        underTest.update(null, CreatePullRequestModel.PROP_TITLE);
        verify(dialogMock).setTitle(any());
        verify(modelMock).getTitle();
        verify(dialogMock, never()).setDescription(anyString());
        verify(modelMock, never()).getDescription();
    }

    @Test
    public void whenLoadingIsChangedOnlyLoadingShouldBeUpdated() {
        underTest.update(null, CreatePullRequestModel.PROP_LOADING);
        verify(dialogMock).setIsLoading(anyBoolean());
        verify(modelMock).isLoading();
        verify(dialogMock, never()).populateDiff(any(Project.class), any(GitChangesContainer.class));
        verify(modelMock, never()).getLocalBranchChanges();
    }
}