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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.event.ActionEvent;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VcsWorkItemsController.class)
public class VcsWorkItemsControllerTest extends IdeaAbstractTest {

    VcsWorkItemsController underTest;

    @Mock
    VcsWorkItemsModel modelMock;
    @Mock
    VcsWorkItemsForm mockForm;
    @Mock
    Project mockProject;
    @Mock
    TabImpl mockTab;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(VcsWorkItemsForm.class).withArguments(mockProject).thenReturn(mockForm);
        PowerMockito.whenNew(VcsWorkItemsModel.class).withArguments(mockProject).thenReturn(modelMock);
        PowerMockito.whenNew(TabImpl.class).withAnyArguments().thenReturn(mockTab);

        underTest = new VcsWorkItemsController(mockProject);
        reset(modelMock);
    }

    @Test
    public void testActionListener_OpenBrowser() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, VcsWorkItemsForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER));
        verify(modelMock).openSelectedItemsLink();
    }

    @Test
    public void testActionListener_CreateBranch() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, VcsWorkItemsForm.CMD_CREATE_BRANCH));
        verify(modelMock).createBranch();
    }

    @Test
    public void testActionListener_QueryChanged() throws Exception {
        underTest.actionPerformed(new ActionEvent(this, 0, WorkItemQueryDropDown.CMD_QUERY_COMBO_BOX_CHANGED));
        verify(modelMock).loadData();
    }
}
