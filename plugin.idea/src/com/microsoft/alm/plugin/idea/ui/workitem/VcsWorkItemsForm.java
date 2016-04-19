// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.ui.components.JBScrollPane;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.FormattedTable;
import com.microsoft.alm.plugin.idea.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabFormImpl;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public class VcsWorkItemsForm extends TabFormImpl {
    private FormattedTable workItemsTable;

    //commands
    public static final String CMD_CREATE_NEW_WORK_ITEM = "createNewWorkItemLink";
    public static final String CMD_OPEN_SELECTED_WIT_IN_BROWSER = "openSelectedWorkItem";
    public static final String TOOLBAR_LOCATION = "Vcs.WorkItems";

    public VcsWorkItemsForm() {
        super(TfPluginBundle.KEY_VCS_WIT_TITLE,
                TfPluginBundle.KEY_VCS_WIT_CREATE_WIT,
                CMD_CREATE_NEW_WORK_ITEM,
                TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP,
                TOOLBAR_LOCATION);

        ensureInitialized();
    }

    protected void createCustomView() {
        workItemsTable = new FormattedTable(WorkItemsTableModel.Column.TITLE.toString());
        workItemsTable.customizeHeader();
        scrollPanel = new JBScrollPane(workItemsTable);
    }

    protected void updateViewOnLoad() {
        // nothing custom needs to be done at this time
    }

    public void setWorkItemsTable(final WorkItemsTableModel tableModel) {
        workItemsTable.setModel(tableModel);
        workItemsTable.setSelectionModel(tableModel.getSelectionModel());
        workItemsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        searchFilter.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            private void onFilterChanged() {
                if (timer.isRunning()) {
                    timer.restart();
                } else {
                    timer.start();
                }
            }
        });

        // Setup table sorter
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
        workItemsTable.setRowSorter(sorter);

        // Attach an index converter to fix the indexes if the user sorts the list
        tableModel.setSelectionConverter(new TableModelSelectionConverter() {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                if (viewRowIndex >= 0) {
                    return workItemsTable.convertRowIndexToModel(viewRowIndex);
                }

                return viewRowIndex;
            }
        });
    }

    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
        addTableEventListeners(listener);
    }

    private void addTableEventListeners(final ActionListener listener) {
        //mouse listener
        workItemsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                //double click
                if (mouseEvent.getClickCount() == 2) {
                    setChangedAndNotify(CMD_OPEN_SELECTED_WIT_IN_BROWSER);
                } else if (mouseEvent.isPopupTrigger() || ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)) {
                    //right click, show pop up
                    showPopupMenu(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY(), listener);
                }
            }
        });

        //keyboard listener
        workItemsTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    setChangedAndNotify(CMD_OPEN_SELECTED_WIT_IN_BROWSER);
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });
    }

    protected List<JBMenuItem> getMenuItems(final ActionListener listener) {
        return Arrays.asList(createMenuItem(TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER, null, VcsWorkItemsForm.CMD_OPEN_SELECTED_WIT_IN_BROWSER, listener));
    }

    @VisibleForTesting
    FormattedTable getWorkItemsTable() {
        return workItemsTable;
    }
}