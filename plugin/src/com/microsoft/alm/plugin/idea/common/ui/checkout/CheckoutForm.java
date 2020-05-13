// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.TableFocusListener;
import com.microsoft.alm.plugin.idea.common.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.common.ui.common.forms.BasicForm;
import com.microsoft.alm.plugin.idea.common.ui.controls.BusySpinnerPanel;
import com.microsoft.alm.plugin.idea.common.ui.controls.HelpPanel;
import com.microsoft.alm.plugin.idea.common.ui.controls.HintTextFieldUI;
import com.microsoft.alm.plugin.idea.common.ui.controls.UserAccountPanel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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

/**
 * This form is hosted by the CheckoutPageImpl.
 */
public class CheckoutForm implements BasicForm, Disposable {
    private JTextField repositoryFilter;
    private JTable repositoryTable;
    private JTextField directoryName;
    private JPanel contentPanel;
    private UserAccountPanel userAccountPanel;
    private TextFieldWithBrowseButton parentDirectory;
    private JButton refreshButton;
    private BusySpinnerPanel busySpinner;
    private JScrollPane repositoryTableScrollPane;
    private HelpPanel helpPanel;
    private JCheckBox advancedCheckBox;
    private JCheckBox serverWorkspaceCheckBox;
    private boolean initialized = false;
    private RepositoryContext.Type repositoryType;
    private Timer timer;

    @NonNls
    public static final String CMD_REPO_FILTER_CHANGED = "repositoryFilterChanged";
    @NonNls
    public static final String CMD_REFRESH = "refresh";
    @NonNls
    public static final String CMD_GOTO_TFS = "gotoTFS";
    @NonNls
    public static final String CMD_GOTO_SPS_PROFILE = "gotoSPSProfile";

    public CheckoutForm(final boolean vsoSelected, final RepositoryContext.Type repositoryType) {
        this.repositoryType = repositoryType;
        // The following call is required to initialize the controls on the form
        // DO NOT MOVE THIS CALL
        $$$setupUI$$$();
        userAccountPanel.setWindowsAccount(!vsoSelected);

        Disposer.register(this, busySpinner);
    }

    @Override
    public void dispose() {}

    public JPanel getContentPanel() {
        ensureInitialized();
        return contentPanel;
    }

    private void ensureInitialized() {
        if (!initialized) {
            // Ensure that the commands are set up correctly
            repositoryFilter.setActionCommand(CMD_REPO_FILTER_CHANGED);
            refreshButton.setActionCommand(CMD_REFRESH);

            // Fix HiDPI scaling for table
            SwingHelper.scaleTableRowHeight(repositoryTable);

            // Fix tabbing in table
            SwingHelper.fixTabKeys(repositoryTable);
            repositoryTable.addFocusListener(new TableFocusListener(repositoryTable));

            // Set help text and popup text
            helpPanel.addPopupCommand(TfPluginBundle.message(TfPluginBundle.KEY_VSO_LOOKUP_HELP_ENTER_URL), CMD_GOTO_TFS);
            helpPanel.addPopupCommand(TfPluginBundle.message(TfPluginBundle.KEY_VSO_LOOKUP_HELP_VIEW_ACCOUNTS), CMD_GOTO_SPS_PROFILE);
            helpPanel.setVisible(false); // Don't show this help panel until we know if it's vs.com

            // Set hint text
            repositoryFilter.setUI(new HintTextFieldUI(
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_FILTER_HINT)));

            // Setup folder browser
            parentDirectory.getInsets().right = 0;
            parentDirectory.addBrowseFolderListener(
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_PARENT_FOLDER_DIALOG_TITLE), null, null,
                    new FileChooserDescriptor(false, true, false, false, false, false));

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
            repositoryFilter.getDocument().addDocumentListener(new DocumentListener() {
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

            repositoryTableScrollPane.setMinimumSize(new Dimension(JBUI.scale(200), JBUI.scale(70)));

            // Initialize the advanced button (only used for TFVC right now)
            advancedCheckBox.setSelected(false);
            serverWorkspaceCheckBox.setSelected(false);
            if (repositoryType == RepositoryContext.Type.TFVC) {
                advancedCheckBox.setVisible(true);
                advancedCheckBox.setText(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TFVC_ADVANCED));

                serverWorkspaceCheckBox.setVisible(true);
                serverWorkspaceCheckBox.setText(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TFVC_SERVER_WORKSPACE));
            } else {
                // There are no advanced features for our Git checkout dialog
                advancedCheckBox.setVisible(false);
                serverWorkspaceCheckBox.setVisible(false);
            }

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

    public void setRepositoryFilter(final String filter) {
        repositoryFilter.setText(filter);
    }

    public String getRepositoryFilter() {
        return repositoryFilter.getText();
    }

    public JComponent getPreferredFocusedComponent() {
        return repositoryFilter;
    }

    public void setRepositoryTable(final ServerContextTableModel tableModel, final ListSelectionModel selectionModel) {
        repositoryTable.setModel(tableModel);
        repositoryTable.setSelectionModel(selectionModel);

        // Setup table sorter
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
        repositoryTable.setRowSorter(sorter);

        // Attach an index converter to fix the indexes if the user sorts the list
        tableModel.setSelectionConverter(new TableModelSelectionConverter() {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                if (viewRowIndex >= 0) {
                    return repositoryTable.convertRowIndexToModel(viewRowIndex);
                }

                return viewRowIndex;
            }
        });
    }

    public void setParentDirectory(final String path) {
        parentDirectory.setText(path);
    }

    public String getParentDirectory() {
        return StringUtils.trim(parentDirectory.getText());
    }

    public JComponent getParentDirectoryComponent() {
        return parentDirectory.getTextField();
    }

    public void setDirectoryName(final String name) {
        directoryName.setText(name);
    }

    public String getDirectoryName() {
        return StringUtils.trim(directoryName.getText());
    }

    public JComponent getDirectoryNameComponent() {
        return directoryName;
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

    public boolean isAdvanced() {
        return advancedCheckBox.isSelected();
    }

    public void setAdvanced(final boolean advanced) {
        advancedCheckBox.setSelected(advanced);
    }

    public boolean isTfvcServerCheckout() {
        return serverWorkspaceCheckBox.isSelected();
    }

    private void createUIComponents() {
        // Create user account panel
        userAccountPanel = new UserAccountPanel();
        refreshButton = new JButton(AllIcons.Actions.Refresh);

        // Create timer for filtering the list
        timer = new Timer(400, null);
        timer.setInitialDelay(400);
        timer.setActionCommand(CMD_REPO_FILTER_CHANGED);
        timer.setRepeats(false);
    }

    @VisibleForTesting
    UserAccountPanel getUserAccountPanel() {
        return this.userAccountPanel;
    }

    @VisibleForTesting
    BusySpinnerPanel getBusySpinner() {
        return this.busySpinner;
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
        contentPanel.setLayout(new GridLayoutManager(10, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.setName("");
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoCheckoutForm.SelectRepository"));
        contentPanel.add(label1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        repositoryFilter = new JTextField();
        repositoryFilter.setName("");
        contentPanel.add(repositoryFilter, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoCheckoutForm.ParentDirectory"));
        contentPanel.add(label2, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoCheckoutForm.DirectoryName"));
        contentPanel.add(label3, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        directoryName = new JTextField();
        directoryName.setName("");
        contentPanel.add(directoryName, new GridConstraints(8, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentPanel.add(userAccountPanel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        repositoryTableScrollPane = new JScrollPane();
        contentPanel.add(repositoryTableScrollPane, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        repositoryTable = new JTable();
        repositoryTable.setFillsViewportHeight(true);
        repositoryTable.setName("");
        repositoryTable.setShowHorizontalLines(false);
        repositoryTable.setShowVerticalLines(false);
        repositoryTableScrollPane.setViewportView(repositoryTable);
        parentDirectory = new TextFieldWithBrowseButton();
        contentPanel.add(parentDirectory, new GridConstraints(6, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        helpPanel = new HelpPanel();
        helpPanel.setHelpText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoLookupHelp.helpText"));
        helpPanel.setPopupText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("VsoLookupHelp.Instructions"));
        contentPanel.add(helpPanel, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        advancedCheckBox = new JCheckBox();
        advancedCheckBox.setText("example text");
        contentPanel.add(advancedCheckBox, new GridConstraints(9, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        busySpinner = new BusySpinnerPanel();
        contentPanel.add(busySpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        refreshButton.setToolTipText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CheckoutDialog.RefreshButton.ToolTip"));
        contentPanel.add(refreshButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
