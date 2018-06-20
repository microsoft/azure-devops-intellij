// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.ui.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBTextField;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolVersionException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import org.apache.commons.lang.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    private JLabel pathLabel;
    private HyperlinkLabel downloadLink;
    private JPanel downloadLinkPane;
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
        downloadLinkPane.setVisible(false);
        downloadLink.setHyperlinkText(
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_LINK_LABEL),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_LINK_TEXT), "");
        downloadLink.setHyperlinkTarget(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_LINK_URL));
        downloadLink.setToolTipText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_LINK_URL));
        pathLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_DESCRIPTION));
        tfExeField.addBrowseFolderListener(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_DESCRIPTION), project,
                new FileChooserDescriptor(true, false, false ,false, false, false)
                        .withFileFilter(new Condition<VirtualFile>() {
                            @Override
                            public boolean value(VirtualFile virtualFile) {
                                return virtualFile.getName().equalsIgnoreCase(SystemInfo.isWindows ? "tf.cmd" : "tf");
                            }
                        })
        );

        testExeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, getCurrentExecutablePath());
                try {
                    TfTool.checkVersion();
                    Messages.showInfoMessage(myContentPane, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_FOUND_EXE), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                    downloadLinkPane.setVisible(false);
                } catch (ToolVersionException e) {
                    Messages.showWarningDialog(myContentPane, LocalizationServiceImpl.getInstance().getExceptionMessage(e), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                    downloadLinkPane.setVisible(true);
                } catch (ToolException e) {
                    Messages.showErrorDialog(myContentPane, LocalizationServiceImpl.getInstance().getExceptionMessage(e), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                    downloadLinkPane.setVisible(true);
                } catch (RuntimeException e) {
                    Messages.showErrorDialog(myContentPane, LocalizationServiceImpl.getInstance().getExceptionMessage(e), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                    downloadLinkPane.setVisible(true);
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

    @VisibleForTesting
    protected String getCurrentExecutablePath() {
        return tfExeField.getText().trim();
    }

    public void load() {
        String tfLocation = PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_TF_HOME);
        tfLocation = StringUtils.isEmpty(tfLocation) ? TfTool.tryDetectTf() : tfLocation;
        // if no path was found then default to an empty string
        originalTfLocation = StringUtils.isEmpty(tfLocation) ? StringUtils.EMPTY : tfLocation;
        if (StringUtils.isEmpty(originalTfLocation)) {
            downloadLinkPane.setVisible(true);
        }
        tfExeField.setText(originalTfLocation);
    }

    public void apply() {
        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, getCurrentExecutablePath());
    }

    public boolean isModified() {
        return !PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_TF_HOME).equals(getCurrentExecutablePath());
    }

    public void reset() {
        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, originalTfLocation);
        tfExeField.setText(originalTfLocation);
    }

    private void createUIComponents() {
        JBTextField textField = new JBTextField();
        textField.getEmptyText().setText(SystemInfo.isWindows
                ? TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_PATH_PLACEHOLDER_WIN)
                : TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_PATH_PLACEHOLDER_NOWIN));
        tfExeField = new TextFieldWithBrowseButton(textField);
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
