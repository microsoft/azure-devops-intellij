/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProjectConfigurableForm {
  private JButton myManageButton;
  private JButton myResetPasswordsButton;
  private final Project myProject;
  private JComponent myContentPane;
  private JCheckBox myUseIdeaHttpProxyCheckBox;

  public ProjectConfigurableForm(final Project project) {
    myProject = project;

    // TODO FIXME remove this line when HttpConfigurable class will be fixed (will not set system properties "http.proxyHost" and others)
    myUseIdeaHttpProxyCheckBox.setVisible(false);

    myManageButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        ManageWorkspacesDialog d = new ManageWorkspacesDialog(myProject);
        d.show();
      }
    });

    myUseIdeaHttpProxyCheckBox.setSelected(TFSConfigurationManager.getInstance().useIdeaHttpProxy());
    myUseIdeaHttpProxyCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        TFSConfigurationManager.getInstance().setUseIdeaHttpProxy(myUseIdeaHttpProxyCheckBox.isSelected());
      }
    });

    myResetPasswordsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final String title = "Reset Stored Passwords";
        if (Messages.showYesNoDialog(myProject, "Do you want to reset all stored passwords?", title, Messages.getQuestionIcon()) == 0) {
          TFSConfigurationManager.getInstance().resetStoredPasswords();
          Messages.showInfoMessage(myProject, "Passwords reset successfully.", title);
        }
      }
    });
  }

  public JComponent getContentPane() {
    return myContentPane;
  }
}
