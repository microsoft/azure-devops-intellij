// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.event.ActionEvent;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VcsPullRequestsControllerTest extends IdeaAbstractTest {
    VcsPullRequestsController underTest;
    VcsPullRequestsModel modelMock;
    VcsPullRequestsTab uiMock;

    @Before
    public void setUp() {
        modelMock = Mockito.mock(VcsPullRequestsModel.class);
        uiMock = Mockito.mock(VcsPullRequestsTab.class);

        underTest = new VcsPullRequestsController();
        underTest.setModel(modelMock);
        underTest.setView(uiMock);
    }

    @Test
    public void testObservableActions() {
        //Add action on toolbar = create pull request
        underTest.update(null, VcsPullRequestsForm.CMD_CREATE_NEW_PULL_REQUEST);
        verify(modelMock).createNewPullRequest();

        //Refresh
        underTest.update(null, VcsPullRequestsForm.CMD_REFRESH);
        verify(modelMock).loadPullRequests();

        //open selected pr
        underTest.update(null, VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER);
        verify(modelMock).openSelectedPullRequestLink();
    }

    @Test
    public void testActionListener() {
        //click on status link
        when(modelMock.isConnected()).thenReturn(true);
        when(modelMock.isAuthenticated()).thenReturn(true);
        underTest.actionPerformed(new ActionEvent(this, 0, VcsPullRequestsForm.CMD_STATUS_LINK));
        verify(modelMock).isConnected();
        verify(modelMock).isAuthenticated();
        verify(modelMock).openGitRepoLink();

        //pop up menu - open in browser
        underTest.actionPerformed(new ActionEvent(this, 0, VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER));
        verify(modelMock).openSelectedPullRequestLink();

        //pop up menu - abandon pr
        underTest.actionPerformed(new ActionEvent(this, 0, VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR));
        verify(modelMock).abandonSelectedPullRequest();

        //pop up menu - complete pr
        underTest.actionPerformed(new ActionEvent(this, 0, VcsPullRequestsForm.CMD_COMPLETE_SELECTED_PR));
        verify(modelMock).completeSelectedPullRequest();
    }

    @Test
    public void testDefaultPropertyUpdates() {
        underTest.update(null, null);
        verify(uiMock).setConnectionStatus(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(modelMock).isLoading();
        verify(modelMock).isConnected();
        verify(modelMock).isAuthenticated();
        verify(modelMock).isAuthenticating();
        verify(modelMock).hasLoadingErrors();
        verify(uiMock).setLastRefreshed(any(Date.class));
        verify(modelMock).getLastRefreshed();
        verify(uiMock).setPullRequestsTree(any(PullRequestsTreeModel.class));
        verify(modelMock).getPullRequestsTreeModel();
    }

    @Test
    public void testPropertyUpdates() {
        //Loading or not
        underTest.update(null, VcsPullRequestsModel.PROP_LOADING);
        verify(uiMock, times(1)).setConnectionStatus(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(modelMock, times(1)).isLoading();

        //Connected or not
        underTest.update(null, VcsPullRequestsModel.PROP_CONNECTED);
        verify(uiMock, times(2)).setConnectionStatus(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(modelMock, times(2)).isConnected();

        //Authenticating or not
        underTest.update(null, VcsPullRequestsModel.PROP_AUTHENTICATING);
        verify(uiMock, times(3)).setConnectionStatus(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(modelMock, times(3)).isAuthenticating();

        //Authenticated or not
        underTest.update(null, VcsPullRequestsModel.PROP_AUTHENTICATED);
        verify(uiMock, times(4)).setConnectionStatus(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(modelMock, times(4)).isAuthenticated();

        //loading errors or not
        underTest.update(null, VcsPullRequestsModel.PROP_LOADING_ERRORS);
        verify(uiMock, times(5)).setConnectionStatus(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(modelMock, times(5)).hasLoadingErrors();

        //Last refreshed date
        underTest.update(null, VcsPullRequestsModel.PROP_LAST_REFRESHED);
        verify(uiMock).setLastRefreshed(any(Date.class));
        verify(modelMock).getLastRefreshed();
    }
}
