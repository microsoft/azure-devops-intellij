// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.JTable;
import java.awt.Color;
import java.awt.event.ActionListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ResolveConflictsFormTest extends IdeaAbstractTest {

    public ResolveConflictsForm resolveConflictsForm;

    @Mock
    public JTable mockTable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resolveConflictsForm = new ResolveConflictsForm();
        resolveConflictsForm.myItemsTable = mockTable;
    }

    @Test
    public void testSetModelForView() {
        ConflictsTableModel conflictsTableModel = new ConflictsTableModel();
        resolveConflictsForm.setModelForView(conflictsTableModel);

        verify(mockTable).setModel(conflictsTableModel);
    }

    @Test
    public void testSetLoading_True() {
        resolveConflictsForm.setLoading(true);

        verify(mockTable).setCellSelectionEnabled(false);
        verify(mockTable).setForeground(Color.GRAY);
        assertFalse(resolveConflictsForm.myAcceptYoursButton.isEnabled());
        assertFalse(resolveConflictsForm.myAcceptTheirsButton.isEnabled());
        assertFalse(resolveConflictsForm.myMergeButton.isEnabled());
        verifyNoMoreInteractions(mockTable);
    }

    @Test
    public void testSetLoading_False() {
        resolveConflictsForm.setLoading(false);

        verify(mockTable).setCellSelectionEnabled(true);
        verify(mockTable).setForeground(Color.BLACK);
        verifyNoMoreInteractions(mockTable);
    }

    @Test
    public void testAddActionListener() {
        ActionListener mockListener = mock(ActionListener.class);
        resolveConflictsForm.addActionListener(mockListener);

        assertEquals(mockListener, resolveConflictsForm.myAcceptYoursButton.getActionListeners()[0]);
        assertEquals(mockListener, resolveConflictsForm.myAcceptTheirsButton.getActionListeners()[0]);
        assertEquals(mockListener, resolveConflictsForm.myMergeButton.getActionListeners()[0]);
    }

    @Test
    public void testEnableButtons_True() {
        resolveConflictsForm.setLoading(false);
        resolveConflictsForm.enableButtons(new int[]{1, 2});

        assertTrue(resolveConflictsForm.myAcceptYoursButton.isEnabled());
        assertTrue(resolveConflictsForm.myAcceptTheirsButton.isEnabled());
        assertTrue(resolveConflictsForm.myMergeButton.isEnabled());
    }

    @Test
    public void testEnableButtons_FalseNoSelection() {
        resolveConflictsForm.setLoading(false);
        resolveConflictsForm.enableButtons(new int[]{});

        assertFalse(resolveConflictsForm.myAcceptYoursButton.isEnabled());
        assertFalse(resolveConflictsForm.myAcceptTheirsButton.isEnabled());
        assertFalse(resolveConflictsForm.myMergeButton.isEnabled());
    }

    @Test
    public void testEnableButtons_FalseLoading() {
        resolveConflictsForm.setLoading(true);
        resolveConflictsForm.enableButtons(new int[]{1, 2});

        assertFalse(resolveConflictsForm.myAcceptYoursButton.isEnabled());
        assertFalse(resolveConflictsForm.myAcceptTheirsButton.isEnabled());
        assertFalse(resolveConflictsForm.myMergeButton.isEnabled());
    }
}