// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.common.FilteredModel;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.awt.event.ActionEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TabControllerImplTest extends IdeaAbstractTest {
    private TabControllerImpl underTest;

    @Mock
    private TabModel mockModel;

    @Mock
    private TabImpl mockTab;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        underTest = new TabControllerImpl(mockTab, mockModel, null) {{
        }};
        Mockito.reset(mockTab);
        Mockito.reset(mockModel);
    }

    @Test
    public void testActionListener_CreateItem() {
        when(mockModel.getTabStatus()).thenReturn(VcsTabStatus.LOADING_COMPLETED);
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_CREATE_NEW_ITEM));
        verify(mockModel).createNewItem();
    }

    @Test
    public void testActionListener_Refresh() {
        when(mockModel.getTabStatus()).thenReturn(VcsTabStatus.LOADING_COMPLETED);
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_REFRESH));
        underTest.update(null, TabForm.CMD_REFRESH);
        verify(mockModel).loadData();
        verify(mockTab).refresh();
    }

    @Test
    public void testActionListener_OpenItem() {
        when(mockModel.getTabStatus()).thenReturn(VcsTabStatus.LOADING_COMPLETED);
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER));
        verify(mockModel).openSelectedItemsLink();
    }

    @Test
    public void testActionListener_LoadingComplete() {
        when(mockModel.getTabStatus()).thenReturn(VcsTabStatus.LOADING_COMPLETED);
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_STATUS_LINK));
        verify(mockModel).openGitRepoLink();
    }

    @Test
    public void testActionListener_NoAuth() {
        when(mockModel.getTabStatus()).thenReturn(VcsTabStatus.NO_AUTH_INFO);
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_STATUS_LINK));
        verify(mockModel).loadData();
    }

    @Test
    public void testActionListener_NotTFS() {
        when(mockModel.getTabStatus()).thenReturn(VcsTabStatus.NOT_TF_GIT_REPO);
        underTest.actionPerformed(new ActionEvent(this, 0, TabForm.CMD_STATUS_LINK));
        verify(mockModel).importIntoTeamServicesGit();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefaultPropertyUpdates() {
        underTest.update(null, null);
        verify(mockTab).setStatus(any(VcsTabStatus.class));
        verify(mockModel).getTabStatus();
        verify(mockTab).setViewModel(any(FilteredModel.class));
        verify(mockModel).getModelForView();
    }

    @Test
    public void testPropertyUpdates() {
        //Tab status updates
        underTest.update(null, TabModel.PROP_TAB_STATUS);
        verify(mockTab, times(1)).setStatus(any(VcsTabStatus.class));
        verify(mockModel, times(1)).getTabStatus();
    }
}
