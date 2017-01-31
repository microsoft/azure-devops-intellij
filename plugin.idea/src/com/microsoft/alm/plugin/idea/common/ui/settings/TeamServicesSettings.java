// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.TableFocusListener;
import com.microsoft.alm.plugin.idea.common.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The general settings page UI
 */
public class TeamServicesSettings {
    private static final Logger logger = LoggerFactory.getLogger(TeamServicesSettings.class);

    private final ServerContextTableModel tableModel;
    private final List<ServerContext> deleteContexts;

    private JPanel mainPanel;
    private JButton updatePasswordButton;
    private JPanel passwordPanel;
    private JTable contextTable;
    private JScrollPane contextScrollPane;
    private JButton deletePasswordButton;

    public TeamServicesSettings() {
        final Project project = IdeaHelper.getCurrentProject();
        deleteContexts = new ArrayList<ServerContext>();

        // initialize UI components
        tableModel = new ServerContextTableModel(ServerContextTableModel.GENERAL_COLUMNS, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        updatePasswordButton.setText(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_UPDATE_BUTTON));
        deletePasswordButton.setText(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DELETE_BUTTON));
        passwordPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_TITLE)), new EmptyBorder(JBUI.scale(10), JBUI.scale(10), JBUI.scale(5), JBUI.scale(10))));
        // Fix HiDPI scaling for table
        SwingHelper.scaleTableRowHeight(contextTable);
        // Fix tabbing in table
        SwingHelper.fixTabKeys(contextTable);
        contextTable.addFocusListener(new TableFocusListener(contextTable));
        contextScrollPane.setMinimumSize(new Dimension(JBUI.scale(200), JBUI.scale(50)));

        updatePasswordButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (tableModel.getSelectedContext() == null) {
                    Messages.showWarningDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE));
                } else {
                    if (Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon()) == Messages.YES) {
                        updateAuth(tableModel.getSelectedContexts(), project);
                    }
                }
            }
        });

        deletePasswordButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (tableModel.getSelectedContext() == null) {
                    Messages.showWarningDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE));
                } else {
                    if (Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon()) == Messages.YES) {
                        // only delete contexts from the table at the moment and not for good (when Apply is used that is when we actually delete the contexts in deleteContexts)
                        deleteContexts.addAll(tableModel.getSelectedContexts());
                        tableModel.removeContexts(tableModel.getSelectedContexts());
                    }
                }
            }
        });
    }

    public JComponent getContentPane() {
        populateContextTable();
        return mainPanel;
    }

    /**
     * Finds the saved server contexts and populates the table with them
     */
    public void populateContextTable() {
        final Collection<ServerContext> serverContexts = ServerContextManager.getInstance().getAllServerContexts();
        tableModel.clearRows();
        tableModel.addServerContexts(new ArrayList<ServerContext>(serverContexts));
        contextTable.setModel(tableModel);
        contextTable.setSelectionModel(tableModel.getSelectionModel());

        // Setup table sorter
        final RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
        contextTable.setRowSorter(sorter);

        // Attach an index converter to fix the indexes if the user sorts the list
        tableModel.setSelectionConverter(new TableModelSelectionConverter() {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                if (viewRowIndex >= 0) {
                    return contextTable.convertRowIndexToModel(viewRowIndex);
                }
                return viewRowIndex;
            }
        });
    }

    /**
     * Apply actually deletes the contexts that the user selected to delete
     */
    public void apply() {
        for (final ServerContext context : deleteContexts) {
            ServerContextManager.getInstance().remove(context.getKey());
        }
        deleteContexts.clear();
    }

    public boolean isModified() {
        return !deleteContexts.isEmpty();
    }

    public void reset() {
        deleteContexts.clear();
        populateContextTable();
    }

    /**
     * Update the auth info for each context selected
     *
     * @param contexts
     * @param project
     */
    private void updateAuth(final List<ServerContext> contexts, final Project project) {
        final Task.Backgroundable updateUathTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_UPDATING)) {
            @Override
            public void run(final ProgressIndicator indicator) {
                final List<ServerContext> newContexts = new ArrayList<ServerContext>(contexts.size());
                for (final ServerContext context : contexts) {
                    final ServerContext newContext = ServerContextManager.getInstance().updateServerContextAuthInfo(context);
                    ServerContextManager.getInstance().remove(context.getKey());
                    ServerContextManager.getInstance().add(newContext);
                    newContexts.add(newContext);
                }
                tableModel.removeContexts(contexts);
                tableModel.addServerContexts(newContexts);
            }
        };
        updateUathTask.queue();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Passwords"));
        passwordPanel = new JPanel();
        passwordPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(passwordPanel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        contextScrollPane = new JScrollPane();
        passwordPanel.add(contextScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        contextTable = new JTable();
        contextTable.setPreferredScrollableViewportSize(new Dimension(450, 150));
        contextScrollPane.setViewportView(contextTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        passwordPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deletePasswordButton = new JButton();
        deletePasswordButton.setText("Delete Password");
        panel1.add(deletePasswordButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        updatePasswordButton = new JButton();
        updatePasswordButton.setText("Update Password");
        panel1.add(updatePasswordButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        mainPanel.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
