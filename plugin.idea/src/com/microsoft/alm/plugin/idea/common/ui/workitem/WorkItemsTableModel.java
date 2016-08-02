// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.workitem;


import com.microsoft.alm.plugin.idea.common.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.common.ui.common.FilteredModel;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * This table model manages a list of workitems. It has a built-in selectionModel as well.
 * TODO refactor this and the other TableModel classes into a generic class (most of the code is the same)
 */
public class WorkItemsTableModel extends AbstractTableModel implements FilteredModel {
    public enum Column {ID, TYPE, TITLE, ASSIGNED_TO, STATE, BRANCH}

    public static final Column[] ALL_COLUMNS = new Column[]{Column.ID, Column.TYPE, Column.TITLE, Column.STATE, Column.ASSIGNED_TO, Column.BRANCH};
    public static final Column[] DEFAULT_COLUMNS = new Column[]{Column.ID, Column.TYPE, Column.STATE, Column.TITLE};
    public static final Column[] COLUMNS_PLUS_BRANCH = new Column[]{Column.ID, Column.TYPE, Column.STATE, Column.BRANCH, Column.TITLE};

    /**
     * The default converter simply returns the index given.
     */
    private final static TableModelSelectionConverter DEFAULT_CONVERTER = new TableModelSelectionConverter() {
        @Override
        public int convertRowIndexToModel(int viewRowIndex) {
            return viewRowIndex;
        }
    };

    private ListSelectionModel selectionModel = new DefaultListSelectionModel();
    private List<WorkItem> rows = new ArrayList<WorkItem>(100);
    private List<WorkItem> filteredRows = null;
    private String filter;
    private final Column[] columns;
    private TableModelSelectionConverter converter;

    public WorkItemsTableModel(final Column[] columns) {
        assert columns != null;
        this.columns = columns.clone();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public WorkItem getWorkItem(final int rowIndex) {
        final List<WorkItem> localRows;
        if (filteredRows != null) {
            localRows = filteredRows;
        } else {
            localRows = rows;
        }

        if (rowIndex >= 0 && rowIndex < localRows.size()) {
            return localRows.get(rowIndex);
        }

        return null;
    }

    public void addWorkItems(final List<WorkItem> workItems) {
        // TODO Remember selection
        //final List<WorkItem> selectedItems = getSelectedWorkItems();

        // Add the new rows to the existing list
        // Note: We don't need to sort them because the server does that
        rows.addAll(workItems);

        if (hasFilter()) {
            // re-apply the filter, this will fire its own event
            applyFilter();
        } else {
            // Fire an event letting callers know
            super.fireTableDataChanged();
        }

        // TODO Attempt to restore the selection
        //select(selectedWorkItems);
    }

    public void setSelectionConverter(final TableModelSelectionConverter converter) {
        this.converter = converter;
    }

    public TableModelSelectionConverter getSelectionConverter() {
        if (converter == null) {
            return DEFAULT_CONVERTER;
        } else {
            return converter;
        }
    }

    public List<WorkItem> getSelectedWorkItems() {
        final List<WorkItem> items = new ArrayList<WorkItem>(this.getRowCount());
        for (int i = 0; i < this.getRowCount(); i++) {
            if (getSelectionModel().isSelectedIndex(i)) {
                items.add(getWorkItem(i));
            }
        }
        return items;
    }

    /* TODO
    public int getSelectedIndex() {
        final int viewSelectedIndex;
        // Check both the max and min selected indexes to see which one is really selected
        if (selectionModel.isSelectedIndex(selectionModel.getMinSelectionIndex())) {
            viewSelectedIndex = selectionModel.getMinSelectionIndex();
        } else {
            viewSelectedIndex = selectionModel.getMaxSelectionIndex();
        }
        final int selectedIndex = getSelectionConverter().convertRowIndexToModel(viewSelectedIndex);
        return selectedIndex;
    }

    public ServerContext getSelectedContext() {
        final ServerContext selectedContext = getServerContext(getSelectedIndex());
        return selectedContext;
    }

    private void select(final ServerContext context) {
        final List<ServerContext> localRows;
        if (filteredRows != null) {
            localRows = filteredRows;
        } else {
            localRows = rows;
        }

        final int index = localRows.indexOf(context);
        if (index >= 0) {
            selectionModel.setSelectionInterval(index, index);
        }
    }
    */

    public void clearRows() {
        filteredRows = null;
        rows.clear();
        super.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        if (filteredRows != null) {
            return filteredRows.size();
        }
        return rows.size();
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final WorkItem item = getWorkItem(rowIndex);
        return getValueFor(item, columnIndex);
    }

    private String getValueFor(final WorkItem item, final int columnIndex) {
        if (item == null) {
            return "";
        }

        // The following might throw index out of bounds, but that is the appropriate error
        final Column column = columns[columnIndex];

        switch (column) {
            case ID:
                return WorkItemHelper.getFieldValue(item, WorkItemHelper.FIELD_ID);

            case TYPE:
                return WorkItemHelper.getFieldValue(item, WorkItemHelper.FIELD_WORK_ITEM_TYPE);

            case STATE:
                return WorkItemHelper.getFieldValue(item, WorkItemHelper.FIELD_STATE);

            case TITLE:
                return WorkItemHelper.getFieldValue(item, WorkItemHelper.FIELD_TITLE);

            case ASSIGNED_TO:
                return WorkItemHelper.getFieldValue(item, WorkItemHelper.FIELD_ASSIGNED_TO);

            case BRANCH:
                return WorkItemHelper.getBranchName(item);

            default:
                return "";
        }
    }

    @Override
    public String getColumnName(final int columnIndex) {
        // The following might throw index out of bounds, but that is the appropriate error
        final Column column = columns[columnIndex];

        switch (column) {
            case ID:
                return WorkItemHelper.getLocalizedFieldName(WorkItemHelper.FIELD_ID);
            case TYPE:
                return WorkItemHelper.getLocalizedFieldName(WorkItemHelper.FIELD_WORK_ITEM_TYPE);
            case TITLE:
                return WorkItemHelper.getLocalizedFieldName(WorkItemHelper.FIELD_TITLE);
            case STATE:
                return WorkItemHelper.getLocalizedFieldName(WorkItemHelper.FIELD_STATE);
            case ASSIGNED_TO:
                return WorkItemHelper.getLocalizedFieldName(WorkItemHelper.FIELD_ASSIGNED_TO);
            case BRANCH:
                return WorkItemHelper.getLocalizedFieldName(WorkItemHelper.BRANCH_ATTRIBUTE_VALUE);
            default:
                return "";
        }
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return false;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    public boolean hasFilter() {
        return StringUtils.isNotEmpty(this.filter);
    }

    public void setFilter(final String filter) {
        this.filter = filter;

        // TODO Remember selection
        //final ServerContext selectedContext = getSelectedContext();

        applyFilter();

        // TODO Attempt to restore the selection
        //select(selectedContext);
    }

    private void applyFilter() {
        if (!hasFilter()) {
            filteredRows = null;
        } else {
            filteredRows = Lists.newArrayList(Collections2.filter(rows, new Predicate<WorkItem>() {
                @Override
                public boolean apply(WorkItem item) {
                    return rowContains(item);
                }
            }));
        }
        super.fireTableDataChanged();
    }

    private boolean rowContains(final WorkItem item) {
        // search for the string in a case insensitive way
        // check each column for a match, if any column contains the string the result is true
        for (int c = 0; c < columns.length; c++) {
            if (StringUtils.containsIgnoreCase(getValueFor(item, c), filter)) {
                return true;
            }
        }

        return false;
    }
}
