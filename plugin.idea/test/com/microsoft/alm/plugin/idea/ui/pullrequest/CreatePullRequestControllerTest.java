// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import git4idea.GitBranch;
import git4idea.GitRemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.swing.ComboBoxModel;
import java.awt.event.ActionEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
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
    public void testActionPerformed() throws Exception {
        ActionEvent e = new ActionEvent(this, 1, CreatePullRequestForm.CMD_TARGET_BRANCH_UPDATED);
        underTest.actionPerformed(e);

        verify(modelMock).setTargetBranch(any(GitRemoteBranch.class));
        verify(modelMock).loadDiff();
    }

    @Test
    public void whenSourceOfUpdateIsNotSetUpdateAll() throws Exception {
        underTest.update(null, null);
        verify(dialogMock).setSourceBranch(any(GitBranch.class));
        verify(dialogMock).setTargetBranchDropdownModel(any(ComboBoxModel.class));
        verify(dialogMock).setSelectedTargetBranch(any(GitRemoteBranch.class));
        verify(dialogMock).setTitle(anyString());
        verify(dialogMock).setDescription(anyString());
        verify(dialogMock).setIsLoading(anyBoolean());
        verify(dialogMock).populateDiff(any(Project.class), any(GitChangesContainer.class));
    }

    /* test some specific updates */
    @Test
    public void whenTitleIsChangedOnlyTitleShouldBeUpdated() {
        underTest.update(null, CreatePullRequestModel.PROP_TITLE);
        verify(dialogMock).setTitle(anyString());
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