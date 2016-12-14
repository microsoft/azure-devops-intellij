// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;

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
        },
        Type(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE)) {
            public String getValue(final Conflict conflict) {
                switch (conflict.getType()) {
                    case CONTENT:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_CONTENT);
                    case RENAME:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_RENAME);
                    case NAME_AND_CONTENT:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_NAME_AND_CONTENT);
                    case MERGE:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_MERGE);
                    case DELETE:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_DELETE);
                    case DELETE_TARGET:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_DELETE_TARGET);
                    case RESOLVED:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_COLUMN_TYPE_RESOLVED);
                }
                return StringUtils.EMPTY;
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

    @Override
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
