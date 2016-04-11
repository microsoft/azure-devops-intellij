// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Font;
import java.util.Enumeration;

/**
 * Creates a custom formatted table
 */
public class FormattedTable extends JBTable {
    private static final int DEFAULT_COLUMN_WIDTH = 200;
    private static final int MAX_ROWS_CHECKED = 100;

    private final String defaultLongColumn;
    private boolean isFormatted = false;
    private final TableModelListener formattedListener = new TableModelListener() {
        @Override
        public void tableChanged(final TableModelEvent e) {
            // formats table when data is first added and then disables itself
            if (shouldFormat()) {
                formatTable();
                getModel().removeTableModelListener(this);
            }
        }
    };

    public FormattedTable() {
        this(null);
    }

    public FormattedTable(final String defaultLongColumn) {
        super();
        this.defaultLongColumn = defaultLongColumn;
        // add height buffer
        setRowHeight(getRowHeight() + JBUI.scale(10));
        getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Customize column headers
     */
    public void customizeHeader() {
        final JTableHeader header = getTableHeader();
        final Font headerFont = header.getFont();
        getTableHeader().setFont(new Font(headerFont.getFontName(), Font.BOLD, headerFont.getSize()));
    }

    @Override
    public void setModel(final TableModel model) {
        super.setModel(model);

        // format if in the correct state
        if (shouldFormat()) {
            formatTable();
        }

        // add listener to format the table once the table contains data
        if (!isFormatted) {
            // if table is not formatted then add a listener so that the table can be formatted once data is in it
            model.addTableModelListener(formattedListener);
        }
    }

    /**
     * Call formatting methods on the table
     */
    private void formatTable() {
        formatColumns();
        setAutoCreateColumnsFromModel(false); // otherwise sizes are recalculated after each TableColumn re-initialization
        isFormatted = true;
    }

    /**
     * Format columns so that they are fit to the data they are holding. If a deafultLongColumn is specified that column
     * will take up any extra space in the table
     */
    private void formatColumns() {
        final Enumeration<TableColumn> columnList = getColumnModel().getColumns();
        while (columnList.hasMoreElements()) {
            final TableColumn column = columnList.nextElement();

            // if column is the defaultLongColumn allow it to fill in the extra table space
            if (defaultLongColumn != null && defaultLongColumn.toLowerCase().equals(column.getHeaderValue().toString().toLowerCase())) {
                column.setPreferredWidth(Short.MAX_VALUE);
            } else {
                int maxWidth = 0;
                // don't check the width of every row to save time if there are many items
                final int maxRowsToCheck = Math.min(MAX_ROWS_CHECKED, getRowCount());
                for (int row = 0; row < maxRowsToCheck; row++) {
                    final TableCellRenderer renderer = getCellRenderer(row, column.getModelIndex());
                    final Component comp = prepareRenderer(renderer, row, column.getModelIndex());
                    maxWidth = Math.max(comp.getPreferredSize().width, maxWidth);
                }

                column.setMinWidth(Math.min(maxWidth + JBUI.scale(10), DEFAULT_COLUMN_WIDTH));
                column.setWidth(column.getMinWidth());
            }
        }
    }

    /**
     * Check to see if table should be formatted based on if it already has been formatted and if the table contains data.
     * Formatting should only take place when there is data in the table because we base column width on the length of the data
     *
     * @return whether the table should be formatted
     */
    private boolean shouldFormat() {
        return !isFormatted && getRowCount() > 0;
    }
}