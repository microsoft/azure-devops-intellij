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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolVersionException;
import com.microsoft.alm.plugin.external.reactive.ReactiveTfClient;
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
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProjectConfigurableForm {
    private static Logger ourLogger = Logger.getInstance(ProjectConfigurableForm.class);

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
    private TextFieldWithBrowseButton reactiveExeField;
    private JButton testReactiveExeButton;
    private String originalTfLocation = StringUtils.EMPTY;
    private String originalReactiveClientLocation = StringUtils.EMPTY;

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
        reactiveExeField.addBrowseFolderListener(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_DESCRIPTION), project,
                new FileChooserDescriptor(true, false, false ,false, false, false)
        );

        testExeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String tfCmdPath = tfExeField.getText();
                if (StringUtils.isEmpty(tfCmdPath)) {
                    Messages.showErrorDialog(myContentPane,
                            TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_PATH_EMPTY),
                            TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                    downloadLink.setVisible(true);
                    return;
                }
                File tfCmdFile = new File(tfCmdPath);
                if (!tfCmdFile.exists() || !tfCmdFile.isFile()) {
                    Messages.showErrorDialog(myContentPane,
                            TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_PATH_NOT_FOUND, tfCmdPath),
                            TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE));
                    downloadLink.setVisible(true);
                    return;
                }
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

        testReactiveExeButton.addActionListener(e -> {
            try {
                ReactiveTfClient client = ReactiveTfClient.create(getCurrentReactiveClientPath());
                client.startAsync().thenComposeAsync(v -> client.checkVersionAsync().thenComposeAsync(isOk -> {
                    if (isOk) {
                        return client.healthCheckAsync().thenAccept(errorMessage -> {
                            if (errorMessage == null) {
                                Messages.showInfoMessage(
                                        myContentPane,
                                        TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_VALID_FOUND),
                                        TfPluginBundle.message(
                                                TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_VERSION_WARNING_TITLE));
                            } else {
                                ourLogger.warn(errorMessage);
                                Messages.showInfoMessage(
                                        myContentPane,
                                        TfPluginBundle.message(
                                                TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_HEALTH_CHECK_ERROR,
                                                errorMessage),
                                        TfPluginBundle.message(
                                                TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_VERSION_WARNING_TITLE));
                            }
                        });
                    } else {
                        Messages.showInfoMessage(
                                myContentPane,
                                TfPluginBundle.message(TfPluginBundle.KEY_REACTIVE_CLIENT_VERSION_TOO_LOW),
                                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_VERSION_WARNING_TITLE));
                        return CompletableFuture.completedFuture(null);
                    }
                })).exceptionally(ex -> {
                    ourLogger.error(ex);
                    Messages.showInfoMessage(
                            myContentPane,
                            LocalizationServiceImpl.getInstance().getExceptionMessage(ex),
                            TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_VERSION_WARNING_TITLE));
                    return null;
                });
            } catch (Throwable ex) {
                ourLogger.error(ex);
                Messages.showInfoMessage(myContentPane, LocalizationServiceImpl.getInstance().getExceptionMessage(ex), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_REACTIVE_CLIENT_VERSION_WARNING_TITLE));
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

    private String getCurrentReactiveClientPath() {
        return reactiveExeField.getText().trim();
    }

    public void load() {
        PropertyService propertyService = PluginServiceProvider.getInstance().getPropertyService();

        String tfLocation = propertyService.getProperty(PropertyService.PROP_TF_HOME);
        tfLocation = StringUtils.isEmpty(tfLocation) ? TfTool.tryDetectTf() : tfLocation;
        // if no path was found then default to an empty string
        originalTfLocation = StringUtils.isEmpty(tfLocation) ? StringUtils.EMPTY : tfLocation;
        if (StringUtils.isEmpty(originalTfLocation)) {
            downloadLinkPane.setVisible(true);
        }
        tfExeField.setText(originalTfLocation);

        String reactiveClientLocation = propertyService.getProperty(PropertyService.PROP_REACTIVE_CLIENT_PATH);
        originalReactiveClientLocation = reactiveClientLocation;
        reactiveExeField.setText(reactiveClientLocation);
    }

    public void apply() {
        PropertyService propertyService = PluginServiceProvider.getInstance().getPropertyService();
        propertyService.setProperty(PropertyService.PROP_TF_HOME, getCurrentExecutablePath());
        propertyService.setProperty(PropertyService.PROP_REACTIVE_CLIENT_PATH, getCurrentReactiveClientPath());
    }

    public boolean isModified() {
        PropertyService propertyService = PluginServiceProvider.getInstance().getPropertyService();
        return !(propertyService.getProperty(PropertyService.PROP_TF_HOME).equals(getCurrentExecutablePath())
                && Objects.equals(propertyService.getProperty(PropertyService.PROP_REACTIVE_CLIENT_PATH), getCurrentReactiveClientPath()));
    }

    public void reset() {
        PropertyService propertyService = PluginServiceProvider.getInstance().getPropertyService();
        propertyService.setProperty(PropertyService.PROP_TF_HOME, originalTfLocation);
        tfExeField.setText(originalTfLocation);

        propertyService.setProperty(PropertyService.PROP_REACTIVE_CLIENT_PATH, originalReactiveClientLocation);
        reactiveExeField.setText(originalReactiveClientLocation);
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
