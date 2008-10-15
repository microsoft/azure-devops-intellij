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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.ui.ManageWorkspacesDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TFSProjectConfigurable implements Configurable {

  private static class WorkspaceInfoComboItem {

    private final WorkspaceInfo myWorkspaceInfo;

    private WorkspaceInfoComboItem(final WorkspaceInfo workspaceInfo) {
      myWorkspaceInfo = workspaceInfo;
    }

    public WorkspaceInfo getWorkspaceInfo() {
      return myWorkspaceInfo;
    }

    public String toString() {
      return getWorkspaceInfo().getName();
    }
  }

  private Project myProject;
  private JComponent myComponent;

  public TFSProjectConfigurable(Project project) {
    myProject = project;
  }

  @Nullable
  @Nls
  public String getDisplayName() {
    return null;
  }

  @Nullable
  public Icon getIcon() {
    return null;
  }

  @Nullable
  @NonNls
  public String getHelpTopic() {
    return null;
  }

  public JComponent createComponent() {
    myComponent = new JPanel(new GridBagLayout());

    GridBagConstraints gc = new GridBagConstraints();
    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;
    gc.insets = new Insets(20, 0, 0, 0);
    gc.anchor = GridBagConstraints.NORTHWEST;
    JButton globalSettingsButton = new JButton("Global settings...");
    myComponent.add(globalSettingsButton, gc);

    globalSettingsButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent e) {
        ManageWorkspacesDialog d = new ManageWorkspacesDialog(myProject);
        d.show();
      }
    });

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;
    gc.gridwidth = 3;
    gc.weightx = 2;
    gc.weighty = 1;
    gc.fill = GridBagConstraints.BOTH;

    myComponent.add(new JPanel(), gc);

    return myComponent;
  }

  public boolean isModified() {
    if (myProject.isDefault()) {
      return false;
    }
    return false; 
  }

  public void apply() throws ConfigurationException {
  }

  public void reset() {
  }

  public void disposeUIResources() {
    myComponent = null;
  }

}
