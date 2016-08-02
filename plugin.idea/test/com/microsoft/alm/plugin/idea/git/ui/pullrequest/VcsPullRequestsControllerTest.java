// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabForm;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.event.ActionEvent;

import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VcsPullRequestsController.class)
public class VcsPullRequestsControllerTest extends IdeaAbstractTest {

    VcsPullRequestsController underTest;

    @Mock
    VcsPullRequestsModel modelMock;
    @Mock
    VcsPullRequestsForm mockForm;
    @Mock
    Project mockProject;
    @Mock
    TabImpl mockTab;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(VcsPullRequestsForm.class).withNoArguments().thenReturn(mockForm);
        PowerMockito.whenNew(VcsPullRequestsModel.class).withArguments(mockProject).thenReturn(modelMock);
        PowerMockito.whenNew(TabImpl.class).withAnyArguments().thenReturn(mockTab);

        underTest = new VcsPullRequestsController(mockProject);
    }

    @Test
    public void testActionListener_OpenBrowser() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER));
        verify(modelMock).openSelectedItemsLink();
    }

    @Test
    public void testActionListener_AbandonPR() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR));
        verify(modelMock).abandonSelectedPullRequest();
    }
}
