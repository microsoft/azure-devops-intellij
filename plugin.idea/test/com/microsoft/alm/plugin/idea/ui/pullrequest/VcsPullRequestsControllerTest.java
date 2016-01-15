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
import static org.mockito.Mockito.verify;

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
    public void testActionPerformed() {
        //Add action on toolbar = create pull request
        final ActionEvent e1 = new ActionEvent(this, 1, VcsPullRequestsForm.CMD_CREATE_NEW_PULL_REQUEST);
        underTest.actionPerformed(e1);
        verify(modelMock).createNewPullRequest();

        //Refresh
        final ActionEvent e2 = new ActionEvent(this, 1, VcsPullRequestsForm.CMD_REFRESH);
        underTest.actionPerformed(e2);
        verify(modelMock).loadPullRequests();
    }

    @Test
    public void testDefaultPropertyUpdates() {
        underTest.update(null, null);
        verify(uiMock).setLoading(anyBoolean());
        verify(modelMock).isLoading();
        verify(uiMock).setConnected(anyBoolean());
        verify(modelMock).isConnected();
        verify(uiMock).setLastRefreshed(any(Date.class));
        verify(modelMock).getLastRefreshed();
        verify(uiMock).setPullRequestsTree(any(PullRequestsTreeModel.class));
        verify(modelMock).getPullRequestsTreeModel();


    }

    @Test
    public void testPropertyUpdates() {
        //Loading or not
        underTest.update(null, VcsPullRequestsModel.PROP_LOADING);
        verify(uiMock).setLoading(anyBoolean());
        verify(modelMock).isLoading();

        //Connected or not
        underTest.update(null, VcsPullRequestsModel.PROP_CONNECTED);
        verify(uiMock).setConnected(anyBoolean());
        verify(modelMock).isConnected();

        //Last refreshed date
        underTest.update(null, VcsPullRequestsModel.PROP_LAST_REFRESHED);
        verify(uiMock).setLastRefreshed(any(Date.class));
        verify(modelMock).getLastRefreshed();

    }
}
