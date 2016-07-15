// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import javax.swing.JTable;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Listens when table is focused on then focus specifically on a row to ease navigation when a row is not already selected
 */
public class TableFocusListener extends FocusAdapter {
    final JTable table;

    public TableFocusListener(final JTable table) {
        super();
        this.table = table;
    }

    @Override
    public void focusGained(FocusEvent focusEvent) {
        super.focusGained(focusEvent);
        if (table.getSelectedRow() == -1) {
            table.setRowSelectionInterval(0, 0);
        }
    }
}
