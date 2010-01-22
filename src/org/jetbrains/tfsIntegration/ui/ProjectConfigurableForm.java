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
  private JCheckBox myTFSCheckBox;
  private JCheckBox myStatefulCheckBox;
  private JCheckBox myReportNotInstalledPoliciesCheckBox;

  public ProjectConfigurableForm(final Project project) {
    myProject = project;

    myManageButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        ManageWorkspacesDialog d = new ManageWorkspacesDialog(myProject);
        d.show();
      }
    });

    myUseIdeaHttpProxyCheckBox.setSelected(TFSConfigurationManager.getInstance().useIdeaHttpProxy());

    myResetPasswordsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final String title = "Reset Stored Passwords";
        if (Messages.showYesNoDialog(myProject, "Do you want to reset all stored passwords?", title, Messages.getQuestionIcon()) == 0) {
          TFSConfigurationManager.getInstance().resetStoredPasswords();
          Messages.showInfoMessage(myProject, "Passwords reset successfully.", title);
        }
      }
    });

    ActionListener l = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateNonInstalledCheckbox();
      }
    };
    myStatefulCheckBox.addActionListener(l);
    myTFSCheckBox.addActionListener(l);
  }

  private void updateNonInstalledCheckbox() {
    if (!myStatefulCheckBox.isSelected() && !myTFSCheckBox.isSelected()) {
      myReportNotInstalledPoliciesCheckBox.setSelected(false);
      myReportNotInstalledPoliciesCheckBox.setEnabled(false);
    }
    else {
      myReportNotInstalledPoliciesCheckBox.setEnabled(true);
    }
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  public boolean useProxy() {
    return myUseIdeaHttpProxyCheckBox.isSelected();
  }

  public void setUserProxy(boolean value) {
    myUseIdeaHttpProxyCheckBox.setSelected(value);
  }

  public boolean supportTfsCheckinPolicies() {
    return myTFSCheckBox.isSelected();
  }

  public boolean supportStatefulCheckinPolicies() {
    return myStatefulCheckBox.isSelected();
  }

  public boolean reportNotInstalledCheckinPolicies() {
    return myReportNotInstalledPoliciesCheckBox.isSelected();
  }

  public void setSupportTfsCheckinPolicies(boolean supportTfsCheckinPolicies) {
    myTFSCheckBox.setSelected(supportTfsCheckinPolicies);
    updateNonInstalledCheckbox();
  }

  public void setSupportStatefulCheckinPolicies(boolean supportStatefulCheckinPolicies) {
    myStatefulCheckBox.setSelected(supportStatefulCheckinPolicies);
    updateNonInstalledCheckbox();
  }

  public void setReportNotInstalledCheckinPolicies(boolean reportNotInstalledCheckinPolicies) {
    myReportNotInstalledPoliciesCheckBox.setSelected(reportNotInstalledCheckinPolicies);
  }

}
