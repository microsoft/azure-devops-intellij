// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.google.common.collect.ImmutableList;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.PageModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.event.ActionEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ResolveConflictControllerTest extends IdeaAbstractTest {

    @Mock
    public ResolveConflictsDialog mockDialog;

    @Mock
    public ResolveConflictsModel mockModel;

    public ResolveConflictsController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = new ResolveConflictsController(mockDialog, mockModel);
    }

    @Test
    public void testConstructor() {
        verify(mockDialog).addActionListener(controller);
        verify(mockModel).addObserver(controller);
        verify(mockModel).loadConflicts();
        verify(mockDialog).setConflictsTableModel(any());
        verify(mockModel).getConflictsTableModel();
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testActionPerformed_AcceptTheirs() {
        reset(mockDialog, mockModel);
        ActionEvent e = new ActionEvent(this, 1, ResolveConflictsForm.CMD_ACCEPT_THEIRS);
        controller.actionPerformed(e);

        verify(mockModel).clearErrors();
        verify(mockDialog).getSelectedRows();
        verify(mockModel).acceptTheirs(any());
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testActionPerformed_AcceptMine() {
        reset(mockDialog, mockModel);
        ActionEvent e = new ActionEvent(this, 1, ResolveConflictsForm.CMD_ACCEPT_YOURS);
        controller.actionPerformed(e);

        verify(mockModel).clearErrors();
        verify(mockDialog).getSelectedRows();
        verify(mockModel).acceptYours(any());
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testActionPerformed_Merge() {
        reset(mockDialog, mockModel);
        ActionEvent e = new ActionEvent(this, 1, ResolveConflictsForm.CMD_MERGE);
        controller.actionPerformed(e);

        verify(mockModel).clearErrors();
        verify(mockDialog).getSelectedRows();
        verify(mockModel).merge(any());
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testActionPerformed_Skip() {
        reset(mockDialog, mockModel);
        ActionEvent e = new ActionEvent(this, 1, BaseDialog.CMD_OK);
        controller.actionPerformed(e);

        verify(mockModel).clearErrors();
        verify(mockModel).processSkippedConflicts();
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testUpdate_Null() {
        reset(mockDialog, mockModel);
        controller.update(null, null);

        verify(mockDialog).setConflictsTableModel(any());
        verify(mockModel).getConflictsTableModel();
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testUpdate_SpecificError() {
        reset(mockDialog, mockModel);
        when(mockModel.hasErrors()).thenReturn(true);
        when(mockModel.getErrors()).thenReturn(ImmutableList.of(ModelValidationInfo.createWithMessage("Error")));
        controller.update(null, PageModel.PROP_ERRORS);

        verify(mockModel).hasErrors();
        verify(mockDialog).displayError("Error");
        verify(mockModel).getErrors();
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testUpdate_NullError() {
        reset(mockDialog, mockModel);
        when(mockModel.hasErrors()).thenReturn(false);
        controller.update(null, PageModel.PROP_ERRORS);

        verify(mockModel).hasErrors();
        verify(mockDialog).displayError(null);
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }
}
