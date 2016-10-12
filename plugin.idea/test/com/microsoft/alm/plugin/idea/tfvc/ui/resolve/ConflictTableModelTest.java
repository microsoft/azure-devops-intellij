// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.google.common.collect.ImmutableList;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class ConflictTableModelTest extends IdeaAbstractTest {

    public ConflictsTableModel model;

    public final List<Conflict> CONFLICTS = ImmutableList.of(new Conflict("/path/to/file1", Conflict.ConflictType.CONTENT),
            new Conflict("/path/to/file2", Conflict.ConflictType.CONTENT),
            new Conflict("/path/to/file3", Conflict.ConflictType.CONTENT));

    @Before
    public void setUp() {
        model = new ConflictsTableModel();
    }

    @Test
    public void testSetConflicts() {
        model.setConflicts(CONFLICTS);

        assertEquals(CONFLICTS, model.getMyConflicts());

        // make sure set clears old contents
        model.setConflicts(Collections.EMPTY_LIST);
        assertEquals(Collections.EMPTY_LIST, model.getMyConflicts());
    }

    @Test
    public void testGetValueAt() {
        model.setConflicts(CONFLICTS);

        assertEquals("/path/to/file1", model.getValueAt(0, 0));
        assertEquals("/path/to/file2", model.getValueAt(1, 0));
        assertEquals("/path/to/file3", model.getValueAt(2, 0));
    }

    @Test
    public void testGetColumnName() {
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_NAME), model.getColumnName(0));
    }

    @Test
    public void testColumnRowCount() {
        model.setConflicts(CONFLICTS);

        assertEquals(CONFLICTS.size(), model.getRowCount());
        assertEquals(1, model.getColumnCount());
    }
}
