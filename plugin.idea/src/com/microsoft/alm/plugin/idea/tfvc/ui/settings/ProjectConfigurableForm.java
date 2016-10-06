// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolVersionException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import org.apache.commons.lang.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProjectConfigurableForm {
    private JButton myManageButton;
    private JButton myResetPasswordsButton;
    private final Project myProject;
    private JComponent myContentPane;
    private JCheckBox myUseIdeaHttpProxyCheckBox;
    private JCheckBox myTFSCheckBox;
    private JCheckBox myStatefulCheckBox;
    private JCheckBox myReportNotInstalledPoliciesCheckBox;
    private JPanel serverLabel;
    private JPanel passwordLabel;
    private JPanel checkinPolicyLabel;
    private JLabel noteLabel;
    private JButton testExeButton;
    private TextFieldWithBrowseButton tfExeField;
    private String originalTfLocation = StringUtils.EMPTY;

    public ProjectConfigurableForm(final Project project) {
        myProject = project;

        // TODO: set these visible once we start using them
        myResetPasswordsButton.setVisible(false);
        myUseIdeaHttpProxyCheckBox.setVisible(false);
        myTFSCheckBox.setVisible(false);
        myStatefulCheckBox.setVisible(false);
        myReportNotInstalledPoliciesCheckBox.setVisible(false);
        myResetPasswordsButton.setVisible(false);
        serverLabel.setVisible(false);
        passwordLabel.setVisible(false);
        checkinPolicyLabel.setVisible(false);
        noteLabel.setVisible(false);

        tfExeField.addBrowseFolderListener(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_DESCRIPTION), project,
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());

        testExeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, getCurrentExecutablePath());
                try {
                    TfTool.checkVersion();
                    Messages.showInfoMessage(myContentPane, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_FOUND_EXE), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                } catch (ToolVersionException e) {
                    Messages.showWarningDialog(myContentPane, LocalizationServiceImpl.getInstance().getExceptionMessage(e), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                } catch (ToolException e) {
                    Messages.showErrorDialog(myContentPane, LocalizationServiceImpl.getInstance().getExceptionMessage(e), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                }

            }
        });

        // load settings
        load();

        // TODO: comment back in when ready to use
//    myManageButton.addActionListener(new ActionListener() {
//      public void actionPerformed(final ActionEvent e) {
//        ManageWorkspacesDialog d = new ManageWorkspacesDialog(myProject);
//        d.show();
//      }
//    });
//
//    myUseIdeaHttpProxyCheckBox.setSelected(TFSConfigurationManager.getInstance().useIdeaHttpProxy());
//
//    myResetPasswordsButton.addActionListener(new ActionListener() {
//      public void actionPerformed(final ActionEvent e) {
//        final String title = "Reset Stored Passwords";
//        if (Messages.showYesNoDialog(myProject, "Do you want to reset all stored passwords?", title, Messages.getQuestionIcon()) == Messages.YES) {
//          TFSConfigurationManager.getInstance().resetStoredPasswords();
//          Messages.showInfoMessage(myProject, "Passwords reset successfully.", title);
//        }
//      }
//    });
//
//        ActionListener l = new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                updateNonInstalledCheckbox();
//            }
//        };
//        myStatefulCheckBox.addActionListener(l);
//        myTFSCheckBox.addActionListener(l);
    }

//    private void updateNonInstalledCheckbox() {
//        if (!myStatefulCheckBox.isSelected() && !myTFSCheckBox.isSelected()) {
//            myReportNotInstalledPoliciesCheckBox.setSelected(false);
//            myReportNotInstalledPoliciesCheckBox.setEnabled(false);
//        } else {
//            myReportNotInstalledPoliciesCheckBox.setEnabled(true);
//        }
//    }

    public JComponent getContentPane() {
        return myContentPane;
    }

    private String getCurrentExecutablePath() {
        return tfExeField.getText().trim();
    }

    public void load() {
        String tfLocation = PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_TF_HOME);
        tfLocation = StringUtils.isEmpty(tfLocation) ? TfTool.tryDetectTf() : tfLocation;
        // if no path was found then default to an empty string
        originalTfLocation = StringUtils.isEmpty(tfLocation) ? StringUtils.EMPTY : tfLocation;
        tfExeField.setText(originalTfLocation);
    }

    public void apply() {
        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, getCurrentExecutablePath());
    }

    public boolean isModified() {
        return (PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_TF_HOME) != getCurrentExecutablePath());
    }

    public void reset() {
        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, originalTfLocation);
        tfExeField.setText(originalTfLocation);
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
        myContentPane = new JPanel();
        myContentPane.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        serverLabel = new JPanel();
        serverLabel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        myContentPane.add(serverLabel, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        serverLabel.setBorder(BorderFactory.createTitledBorder("Servers and workspaces"));
        myManageButton = new JButton();
        myManageButton.setText("Manage...");
        myManageButton.setMnemonic('M');
        myManageButton.setDisplayedMnemonicIndex(0);
        serverLabel.add(myManageButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        serverLabel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myUseIdeaHttpProxyCheckBox = new JCheckBox();
        myUseIdeaHttpProxyCheckBox.setEnabled(true);
        myUseIdeaHttpProxyCheckBox.setSelected(false);
        myUseIdeaHttpProxyCheckBox.setText("Use HTTP Proxy settings");
        myUseIdeaHttpProxyCheckBox.setMnemonic('U');
        myUseIdeaHttpProxyCheckBox.setDisplayedMnemonicIndex(0);
        serverLabel.add(myUseIdeaHttpProxyCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        passwordLabel = new JPanel();
        passwordLabel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        myContentPane.add(passwordLabel, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        passwordLabel.setBorder(BorderFactory.createTitledBorder("Passwords"));
        final Spacer spacer2 = new Spacer();
        passwordLabel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myResetPasswordsButton = new JButton();
        myResetPasswordsButton.setText("Reset Saved Passwords");
        myResetPasswordsButton.setMnemonic('R');
        myResetPasswordsButton.setDisplayedMnemonicIndex(0);
        passwordLabel.add(myResetPasswordsButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkinPolicyLabel = new JPanel();
        checkinPolicyLabel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        myContentPane.add(checkinPolicyLabel, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkinPolicyLabel.setBorder(BorderFactory.createTitledBorder("Checkin policies compatibility"));
        myReportNotInstalledPoliciesCheckBox = new JCheckBox();
        myReportNotInstalledPoliciesCheckBox.setText("Warn about not installed policies");
        myReportNotInstalledPoliciesCheckBox.setMnemonic('W');
        myReportNotInstalledPoliciesCheckBox.setDisplayedMnemonicIndex(0);
        checkinPolicyLabel.add(myReportNotInstalledPoliciesCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        noteLabel = new JLabel();
        noteLabel.setText("(Note: these settings may be overridden for individual team project)");
        checkinPolicyLabel.add(noteLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myTFSCheckBox = new JCheckBox();
        myTFSCheckBox.setText("Evaluate Team Explorer policies");
        myTFSCheckBox.setMnemonic('T');
        myTFSCheckBox.setDisplayedMnemonicIndex(9);
        checkinPolicyLabel.add(myTFSCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myStatefulCheckBox = new JCheckBox();
        myStatefulCheckBox.setText("Evaluate Teamprise policies");
        myStatefulCheckBox.setMnemonic('E');
        myStatefulCheckBox.setDisplayedMnemonicIndex(10);
        checkinPolicyLabel.add(myStatefulCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Path to tf executable:");
        myContentPane.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tfExeField = new TextFieldWithBrowseButton();
        myContentPane.add(tfExeField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        testExeButton = new JButton();
        testExeButton.setText("Test");
        myContentPane.add(testExeButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        myContentPane.add(spacer3, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myContentPane;
    }

//    public boolean useProxy() {
//        return myUseIdeaHttpProxyCheckBox.isSelected();
//    }
//
//    public void setUserProxy(boolean value) {
//        myUseIdeaHttpProxyCheckBox.setSelected(value);
//    }
//
//    public boolean supportTfsCheckinPolicies() {
//        return myTFSCheckBox.isSelected();
//    }
//
//    public boolean supportStatefulCheckinPolicies() {
//        return myStatefulCheckBox.isSelected();
//    }
//
//    public boolean reportNotInstalledCheckinPolicies() {
//        return myReportNotInstalledPoliciesCheckBox.isSelected();
//    }
//
//    public void setSupportTfsCheckinPolicies(boolean supportTfsCheckinPolicies) {
//        myTFSCheckBox.setSelected(supportTfsCheckinPolicies);
//        updateNonInstalledCheckbox();
//    }
//
//    public void setSupportStatefulCheckinPolicies(boolean supportStatefulCheckinPolicies) {
//        myStatefulCheckBox.setSelected(supportStatefulCheckinPolicies);
//        updateNonInstalledCheckbox();
//    }
//
//    public void setReportNotInstalledCheckinPolicies(boolean reportNotInstalledCheckinPolicies) {
//        myReportNotInstalledPoliciesCheckBox.setSelected(reportNotInstalledCheckinPolicies);
//    }

}
