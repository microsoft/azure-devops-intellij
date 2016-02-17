// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;


import com.google.common.annotations.VisibleForTesting;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.ui.common.forms.BasicForm;
import com.microsoft.alm.plugin.idea.ui.controls.BusySpinnerPanel;
import com.microsoft.alm.plugin.idea.ui.controls.HelpPanel;
import com.microsoft.alm.plugin.idea.ui.controls.HintTextFieldUI;
import com.microsoft.alm.plugin.idea.ui.controls.UserAccountPanel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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

public class ImportForm implements BasicForm {
    private JPanel contentPanel;
    private UserAccountPanel userAccountPanel;
    private JTextField teamProjectFilter;
    private JTable teamProjectTable;
    private JTextField repositoryName;
    private JButton refreshButton;
    private BusySpinnerPanel busySpinner;
    private JScrollPane teamProjectScrollPane;
    private HelpPanel helpPanel;

    private boolean initialized = false;
    private Timer timer;

    @NonNls
    public static final String CMD_PROJECT_FILTER_CHANGED = "teamProjectFilterChanged";
    @NonNls
    public static final String CMD_REFRESH = "refresh";
    @NonNls
    public static final String CMD_GOTO_TFS = "gotoTFS";
    @NonNls
    public static final String CMD_GOTO_SPS_PROFILE = "gotoSPSProfile";

    public ImportForm(final boolean vsoSelected) {
        // The following call is required to initialize the controls on the form
        // DO NOT MOVE THIS CALL
        $$$setupUI$$$();
        userAccountPanel.setWindowsAccount(!vsoSelected);
    }

    public JPanel getContentPanel() {
        ensureInitialized();
        return contentPanel;
    }

    private void ensureInitialized() {
        if (!initialized) {
            // Ensure that the commands are set up correctly
            teamProjectFilter.setActionCommand(CMD_PROJECT_FILTER_CHANGED);
            refreshButton.setActionCommand(CMD_REFRESH);

            // Fix HiDPI scaling for table
            SwingHelper.scaleTableRowHeight(teamProjectTable);

            // Fix tabbing in table
            SwingHelper.fixTabKeys(teamProjectTable);

            // Set help text and popup text
            helpPanel.addPopupCommand(TfPluginBundle.message(TfPluginBundle.KEY_VSO_LOOKUP_HELP_ENTER_URL), CMD_GOTO_TFS);
            helpPanel.addPopupCommand(TfPluginBundle.message(TfPluginBundle.KEY_VSO_LOOKUP_HELP_VIEW_ACCOUNTS), CMD_GOTO_SPS_PROFILE);
            helpPanel.setVisible(false); // Don't show this help panel until we know if it's vs.com

            // Set hint text
            teamProjectFilter.setUI(new HintTextFieldUI(
                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_FILTER_HINT)));

            // Align the busy spinner and the refresh button with the height of the text box
            refreshButton.putClientProperty("JButton.buttonType", "square"); // This is a magical property that tells IntelliJ to draw the button like an image button
            final int textBoxHeight = (int) teamProjectFilter.getPreferredSize().getHeight();
            final Dimension size = new Dimension(textBoxHeight, textBoxHeight);
            refreshButton.setMinimumSize(size);
            refreshButton.setPreferredSize(size);
            busySpinner.setMinimumSize(size);
            busySpinner.setPreferredSize(size);

            // Setup document events for filter
            // Using a timer so that we don't respond to every character typed
            // The timer is created in the create components method
            teamProjectFilter.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    onFilterChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    onFilterChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
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

            teamProjectScrollPane.setMinimumSize(new Dimension(JBUI.scale(200), JBUI.scale(70)));
            teamProjectTable.setRowHeight(teamProjectTable.getFontMetrics(teamProjectTable.getFont()).getHeight());

            initialized = true;
        }
    }

    public void addActionListener(final ActionListener listener) {
        // Hook up listener to all actions
        userAccountPanel.addActionListener(listener);
        timer.addActionListener(listener);
        refreshButton.addActionListener(listener);
        helpPanel.addActionListener(listener);
    }

    public void setTeamProjectFilter(final String filter) {
        teamProjectFilter.setText(filter);
    }

    public String getTeamProjectFilter() {
        return teamProjectFilter.getText();
    }

    public JComponent getPreferredFocusedComponent() {
        return teamProjectFilter;
    }

    public void setTeamProjectTable(final ServerContextTableModel tableModel, final ListSelectionModel selectionModel) {
        teamProjectTable.setModel(tableModel);
        teamProjectTable.setSelectionModel(selectionModel);

        // Setup table sorter
        final RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
        teamProjectTable.setRowSorter(sorter);

        // Attach an index converter to fix the indexes if the user sorts the list
        tableModel.setSelectionConverter(new TableModelSelectionConverter() {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                if (viewRowIndex >= 0) {
                    return teamProjectTable.convertRowIndexToModel(viewRowIndex);
                }

                return viewRowIndex;
            }
        });
    }

    public void setRepositoryName(final String name) {
        repositoryName.setText(name);
    }

    public String getRepositoryName() {
        return StringUtils.trim(repositoryName.getText());
    }

    public JComponent getRepositoryNameComponent() {
        return repositoryName;
    }

    public void setUserName(final String name) {
        userAccountPanel.setUserName(name);
    }

    public void setServerName(final String name) {
        userAccountPanel.setServerName(name);

        // show the helpPanel if the server is a VSO server
        if (!userAccountPanel.isWindowsAccount() || UrlHelper.isTeamServicesUrl(name)) {
            helpPanel.setVisible(true);
        } else {
            helpPanel.setVisible(false);
        }
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

    private void createUIComponents() {
        userAccountPanel = new UserAccountPanel();

        // Create timer for filtering the list
        timer = new Timer(400, null);
        timer.setInitialDelay(400);
        timer.setActionCommand(CMD_PROJECT_FILTER_CHANGED);
        timer.setRepeats(false);
    }

    @VisibleForTesting
    BusySpinnerPanel getBusySpinner() {
        return this.busySpinner;
    }

    @VisibleForTesting
    UserAccountPanel getUserAccountPanel() {
        return this.userAccountPanel;
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
        contentPanel.setLayout(new GridLayoutManager(7, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(userAccountPanel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("ImportForm.SelectTeamProject"));
        contentPanel.add(label1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        teamProjectFilter = new JTextField();
        contentPanel.add(teamProjectFilter, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        teamProjectScrollPane = new JScrollPane();
        contentPanel.add(teamProjectScrollPane, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        teamProjectTable = new JTable();
        teamProjectTable.setFillsViewportHeight(true);
        teamProjectTable.setShowHorizontalLines(false);
        teamProjectTable.setShowVerticalLines(false);
        teamProjectScrollPane.setViewportView(teamProjectTable);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("ImportForm.NewRepositoryName"));
        contentPanel.add(label2, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        repositoryName = new JTextField();
        contentPanel.add(repositoryName, new GridConstraints(6, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        refreshButton = new JButton();
        refreshButton.setIcon(new ImageIcon(getClass().getResource("/actions/refresh.png")));
        refreshButton.setText("");
        refreshButton.setToolTipText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("ImportDialog.RefreshButton.ToolTip"));
        contentPanel.add(refreshButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        busySpinner = new BusySpinnerPanel();
        contentPanel.add(busySpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        helpPanel = new HelpPanel();
        helpPanel.setHelpText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoLookupHelp.helpText"));
        helpPanel.setPopupText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoLookupHelp.Instructions"));
        contentPanel.add(helpPanel, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }
}
