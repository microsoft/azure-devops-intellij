// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTable;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Listens when table is focused on then focus specifically on a row to ease navigation when a row is not already selected
 */
public class TableFocusListener extends FocusAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TableFocusListener.class);
    final JTable table;

    public TableFocusListener(final JTable table) {
        super();
        this.table = table;
    }

    @Override
    public void focusGained(final FocusEvent focusEvent) {
        super.focusGained(focusEvent);
        try {
            if (table.getRowCount() > 0 && table.getSelectedRow() == -1) {
                table.setRowSelectionInterval(0, 0);
            }
        } catch (final Throwable t) {
            // Log the message and swallow it. We don't want to leak exceptions from this event.
            logger.warn("Error on focusGained.", t);
        }
    }
}
