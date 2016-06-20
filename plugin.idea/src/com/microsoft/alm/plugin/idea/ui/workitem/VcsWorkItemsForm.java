// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.controls.WorkItemQueryDropDown;
import com.microsoft.alm.plugin.idea.ui.controls.FormattedTable;
import com.microsoft.alm.plugin.idea.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabFormImpl;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public class VcsWorkItemsForm extends TabFormImpl<WorkItemsTableModel> {
    private final WorkItemQueryDropDown queryDropDown;
    private FormattedTable workItemsTable;

    //commands
    public static final String CMD_CREATE_BRANCH = "createBranch";
    public static final String TOOLBAR_LOCATION = "Vcs.WorkItems";

    public VcsWorkItemsForm(final @NotNull Project project) {
        super(TfPluginBundle.KEY_VCS_WIT_TITLE,
                TfPluginBundle.KEY_VCS_WIT_CREATE_WIT,
                TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP,
                TOOLBAR_LOCATION);

        this.queryDropDown = new WorkItemQueryDropDown(TfGitHelper.getTfGitRepository(project));
        ensureInitialized();
    }

    @VisibleForTesting
    protected VcsWorkItemsForm(final WorkItemQueryDropDown queryDropDown) {
        super(TfPluginBundle.KEY_VCS_WIT_TITLE,
                TfPluginBundle.KEY_VCS_WIT_CREATE_WIT,
                TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP,
                TOOLBAR_LOCATION);
        this.queryDropDown = queryDropDown;
    }

    protected void createCustomView() {
        workItemsTable = new FormattedTable(WorkItemsTableModel.Column.TITLE.toString());
        workItemsTable.customizeHeader();
        scrollPanel = new JBScrollPane(workItemsTable);
    }

    protected void addCustomTools(final JPanel toolBar) {
        toolBar.add(Box.createRigidArea(new Dimension(JBUI.scale(14), 0))); // add padding between components
        toolBar.add(queryDropDown);
    }

    public void setModelForView(final WorkItemsTableModel tableModel) {
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
        queryDropDown.addActionListener(listener);
    }

    private void addTableEventListeners(final ActionListener listener) {
        //mouse listener
        workItemsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                //double click
                if (mouseEvent.getClickCount() == 2) {
                    triggerEvent(CMD_OPEN_SELECTED_ITEM_IN_BROWSER);
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
                    triggerEvent(CMD_OPEN_SELECTED_ITEM_IN_BROWSER);
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });
    }

    protected List<JBMenuItem> getMenuItems(final ActionListener listener) {
        return Arrays.asList(
                createMenuItem(TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER, null, CMD_OPEN_SELECTED_ITEM_IN_BROWSER, listener),
                createMenuItem(TfPluginBundle.KEY_VCS_WIT_CREATE_BRANCH, null, CMD_CREATE_BRANCH, listener));
    }

    public Operation.Inputs getOperationInputs() {
        return new WorkItemLookupOperation.WitInputs(queryDropDown.getSelectedResults());
    }

    public void refresh() {
        queryDropDown.refreshDropDown();
    }

    @VisibleForTesting
    FormattedTable getWorkItemsTable() {
        return workItemsTable;
    }
}