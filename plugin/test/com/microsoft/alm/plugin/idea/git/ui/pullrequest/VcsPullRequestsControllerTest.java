// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabForm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VcsPullRequestsControllerTest extends IdeaAbstractTest {

    VcsPullRequestsController underTest;

    @Mock
    Project mockProject;

    @Mock
    private MockedConstruction<VcsPullRequestsModel> vcsPullRequestsModelConstruction;

    @SuppressWarnings("unused") // used to avoid triggering UI-related code in these tests
    @Mock
    private MockedConstruction<VcsPullRequestsForm> vcsPullRequestsFormConstruction;

    @Before
    public void setUp() throws Exception {
        underTest = new VcsPullRequestsController(mockProject);
    }

    private VcsPullRequestsModel getModelMock() {
        var models = vcsPullRequestsModelConstruction.constructed();
        assertEquals(1, models.size());
        return models.get(0);
    }

    @Test
    public void testActionListener_OpenBrowser() {
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER));
        verify(getModelMock()).openSelectedItemsLink();
    }

    @Test
    public void testActionListener_AbandonPR() {
        underTest.actionPerformed(new ActionEvent(this, 0, VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR));
        verify(getModelMock()).abandonSelectedPullRequest();
    }
}
