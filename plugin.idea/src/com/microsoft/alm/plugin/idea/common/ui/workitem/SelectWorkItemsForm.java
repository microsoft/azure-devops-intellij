// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.workitem;

import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.TableFocusListener;
import com.microsoft.alm.plugin.idea.common.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.common.ui.controls.BusySpinnerPanel;
import com.microsoft.alm.plugin.idea.common.ui.controls.FormattedTable;
import com.microsoft.alm.plugin.idea.common.ui.controls.HelpPanel;
import com.microsoft.alm.plugin.idea.common.ui.controls.HintTextFieldUI;
import com.microsoft.alm.plugin.idea.common.ui.controls.Hyperlink;
import org.jetbrains.annotations.NonNls;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

public class SelectWorkItemsForm {
    private JLabel serverName;
    private JTextField filter;
    private JButton refreshButton;
    private FormattedTable workItemTable;
    private Hyperlink newWorkItemLink;
    private JPanel contentPanel;
    private BusySpinnerPanel busySpinner;
    private JScrollPane scrollPane;
    private HelpPanel helpPanel;
    private boolean initialized = false;
    private Timer timer;

    @NonNls
    public static final String CMD_FILTER_CHANGED = "filterChanged";
    @NonNls
    public static final String CMD_REFRESH = "refresh";
    @NonNls
    public static final String CMD_NEW_WORK_ITEM = "newWorkItem";
    @NonNls
    public static final String CMD_GOTO_VIEW_MY_WORK_ITEMS = "gotoViewMyWorkItems";

    public SelectWorkItemsForm() {
        $$$setupUI$$$();
        // Create timer for filtering the list
        timer = new Timer(400, null);
        timer.setInitialDelay(400);
        timer.setActionCommand(CMD_FILTER_CHANGED);
        timer.setRepeats(false);
    }

    public JPanel getContentPanel() {
        ensureInitialized();
        return contentPanel;
    }

    private void ensureInitialized() {
        if (!initialized) {
            // Ensure that the commands are set up correctly
            filter.setActionCommand(CMD_FILTER_CHANGED);
            refreshButton.setActionCommand(CMD_REFRESH);
            newWorkItemLink.setActionCommand(CMD_NEW_WORK_ITEM);

            // Fix HiDPI scaling for table
            SwingHelper.scaleTableRowHeight(workItemTable);

            // Fix tabbing in table
            SwingHelper.fixTabKeys(workItemTable);
            workItemTable.addFocusListener(new TableFocusListener(workItemTable));

            // Set help text and popup text
            helpPanel.addPopupCommand(TfPluginBundle.message(TfPluginBundle.KEY_VIEW_MY_WORK_ITEMS), CMD_GOTO_VIEW_MY_WORK_ITEMS);
            helpPanel.setVisible(false); // Don't show this help panel until we need to

            // Set hint text
            filter.setUI(new HintTextFieldUI(TfPluginBundle.message(TfPluginBundle.KEY_WIT_SELECT_DIALOG_FILTER_HINT_TEXT)));

            // Align the busy spinner with the height of the refresh button (has to be the refresh button height so the image isn't squashed and then disappears)
            // Also change the refresh button width so that the button is a perfect square
            final int refreshButtonHeight = (int) refreshButton.getMinimumSize().getHeight();
            final Dimension size = new Dimension(refreshButtonHeight, refreshButtonHeight);
            refreshButton.setMinimumSize(size);
            refreshButton.setPreferredSize(size);
            busySpinner.setMinimumSize(size);
            busySpinner.setPreferredSize(size);

            // Setup document events for filter
            // Using a timer so that we don't respond to every character typed
            // The timer is created in the create components method
            filter.getDocument().addDocumentListener(new DocumentListener() {
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

            scrollPane.setMinimumSize(new Dimension(JBUI.scale(600), JBUI.scale(400)));

            initialized = true;
        }
    }

    public void addActionListener(final ActionListener listener) {
        // Hook up listener to all actions
        timer.addActionListener(listener);
        refreshButton.addActionListener(listener);
        newWorkItemLink.addActionListener(listener);
        helpPanel.addActionListener(listener);
    }

    public void setFilter(final String filterString) {
        filter.setText(filterString);
    }

    public String getFilter() {
        return filter.getText();
    }

    public JComponent getPreferredFocusedComponent() {
        return filter;
    }

    public void setWorkItemTable(final WorkItemsTableModel tableModel, final ListSelectionModel selectionModel) {
        workItemTable.setModel(tableModel);
        workItemTable.setSelectionModel(selectionModel);

        // Setup table sorter
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
        workItemTable.setRowSorter(sorter);

        // Attach an index converter to fix the indexes if the user sorts the list
        tableModel.setSelectionConverter(new TableModelSelectionConverter() {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                if (viewRowIndex >= 0) {
                    return workItemTable.convertRowIndexToModel(viewRowIndex);
                }

                return viewRowIndex;
            }
        });
    }

    public void setServerName(final String name) {
        serverName.setText(name);
    }

    public void setLoading(final boolean loading) {
        if (loading) {
            refreshButton.setVisible(false);
            busySpinner.start(true);
        } else {
            busySpinner.stop(true);
            refreshButton.setVisible(true);
        }
    }

    public void setShowHelpPanel(boolean show) {
        helpPanel.setVisible(show);
    }

    private void createUIComponents() {
        workItemTable = new FormattedTable(WorkItemsTableModel.Column.TITLE.toString());
        refreshButton = new JButton(AllIcons.Actions.Refresh);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(6, 4, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Server: ");
        contentPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverName = new JLabel();
        serverName.setText("Label");
        contentPanel.add(serverName, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filter = new JTextField();
        contentPanel.add(filter, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        refreshButton.setText("");
        refreshButton.setToolTipText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CheckoutDialog.RefreshButton.ToolTip"));
        contentPanel.add(refreshButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        contentPanel.add(scrollPane, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        workItemTable.setFillsViewportHeight(true);
        scrollPane.setViewportView(workItemTable);
        newWorkItemLink = new Hyperlink();
        newWorkItemLink.setText("Create a new work item...");
        contentPanel.add(newWorkItemLink, new GridConstraints(5, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        contentPanel.add(spacer1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        busySpinner = new BusySpinnerPanel();
        contentPanel.add(busySpinner, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        helpPanel = new HelpPanel();
        helpPanel.setHelpText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("WitSelectDialog.Help.HelpText"));
        helpPanel.setPopupText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("WitSelectDialog.Help.Instructions"));
        contentPanel.add(helpPanel, new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }
}
