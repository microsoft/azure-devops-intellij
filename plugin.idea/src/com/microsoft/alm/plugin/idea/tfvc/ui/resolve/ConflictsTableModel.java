// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model that displays the list of conflicts to the user
 */
public class ConflictsTableModel extends AbstractTableModel {

    public enum Column {

        Name(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_NAME)) {
            public String getValue(final Conflict conflict) {
                return conflict.getLocalPath();
            }
        };

        private final String myCaption;

        Column(final String caption) {
            myCaption = caption;
        }

        public String getCaption() {
            return myCaption;
        }

        public abstract String getValue(final Conflict conflict);

    }

    private List<Conflict> myConflicts = new ArrayList<Conflict>();

    public String getColumnName(final int column) {
        return Column.values()[column].getCaption();
    }

    public int getRowCount() {
        return myConflicts != null ? myConflicts.size() : 0;
    }

    public int getColumnCount() {
        return Column.values().length;
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final Conflict conflict = myConflicts.get(rowIndex);
        return Column.values()[columnIndex].getValue(conflict);
    }

    public void setConflicts(final List<Conflict> conflicts) {
        myConflicts.clear();
        myConflicts.addAll(conflicts);
        fireTableDataChanged();
    }

    public List<Conflict> getMyConflicts() {
        return myConflicts;
    }
}
