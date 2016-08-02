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

package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;

public class SummaryForm {
  private JLabel myServerLabel;
  private JLabel myWorkspaceLabel;
  private JLabel mySourceLabel;
  private JLabel myDestinationLabel;
  private JPanel myContentPanel;
  private JLabel myWorkspaceTypeLabel;

  public void setServer(ServerInfo server) {
    myServerLabel.setText(server.getPresentableUri());
  }

  public void setWorkspace(WorkspaceInfo workspace) {
    myWorkspaceLabel.setText(workspace.getName());
    myWorkspaceTypeLabel.setText("Existing workspace to be used:");
  }

  public void setNewWorkspaceName(String newWorkspaceName) {
    myWorkspaceLabel.setText(newWorkspaceName);
    myWorkspaceTypeLabel.setText("Workspace to be created:");
  }

  public void setServerPath(String path) {
    mySourceLabel.setText(path);
  }

  public void setLocalPath(final @NotNull String path) {
    myDestinationLabel.setText(path);
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

}
