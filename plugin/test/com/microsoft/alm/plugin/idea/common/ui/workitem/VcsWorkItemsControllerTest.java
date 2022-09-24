// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabImpl;
import com.microsoft.alm.plugin.idea.common.ui.controls.WorkItemQueryDropDown;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;

import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VcsWorkItemsControllerTest extends IdeaAbstractTest {

    VcsWorkItemsController underTest;

    @Mock
    Project mockProject;
    @Mock
    TabImpl mockTab;

    @Mock
    private MockedConstruction<VcsWorkItemsForm> vcsWorkItemsFormConstruction;

    @Mock
    private MockedConstruction<VcsWorkItemsModel> vcsWorkItemsModelConstruction;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(TabImpl.class).withAnyArguments().thenReturn(mockTab);

        underTest = new VcsWorkItemsController(mockProject);
    }

    private VcsWorkItemsModel getModelMock() {
        var models = vcsWorkItemsModelConstruction.constructed();
        assertEquals(1, models.size());
        return models.get(0);
    }

    @Test
    public void testActionListener_OpenBrowser() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, VcsWorkItemsForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER));
        verify(getModelMock()).openSelectedItemsLink();
    }

    @Test
    public void testActionListener_CreateBranch() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, VcsWorkItemsForm.CMD_CREATE_BRANCH));
        verify(getModelMock()).createBranch();
    }

    @Test
    public void testActionListener_QueryChanged() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, WorkItemQueryDropDown.CMD_QUERY_COMBO_BOX_CHANGED));
        verify(getModelMock(), times(2)).loadData();
    }
}
