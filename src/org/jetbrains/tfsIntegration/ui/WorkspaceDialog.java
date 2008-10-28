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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import java.util.List;

public class WorkspaceDialog extends DialogWrapper {

  private final Project myProject;
  private final @Nullable WorkspaceInfo myWorkspace;
  private final @NotNull ServerInfo myServer;
  private WorkspaceForm myForm;

  public WorkspaceDialog(Project project, @NotNull ServerInfo server) {
    super(project, true);
    myProject = project;
    myServer = server;
    myWorkspace = null;
    init();
  }

  public WorkspaceDialog(Project project, @NotNull WorkspaceInfo workspace) {
    super(project, true);
    myProject = project;
    myServer = workspace.getServer();
    myWorkspace = workspace;
    init();
  }

  protected void init() {
    super.init();
    setResizable(true);
    setTitle(myWorkspace != null ? "Edit Workspace" : "Create Workspace");
    setOKButtonText("Save");
    setOKActionEnabled(myWorkspace != null);
  }

  protected JComponent createCenterPanel() {
    myForm = new WorkspaceForm(myProject);
    try {
      if (myWorkspace != null) {
        myForm.init(myWorkspace);
      }
      else {
        myForm.init(myServer);
      }
    }
    catch (TfsException e) {
      return null;
    }

    myForm.addListener(new WorkspaceForm.Listener() {
      public void dataChanged() {
        String errorMessage = validate(myForm.getWorkspaceName());
        setOKActionEnabled(errorMessage == null);
        myForm.setErrorMessage(errorMessage);
      }
    });
    return myForm.getContentPane();
  }

  @Nullable
  private static String validate(String workspaceName) {
    if (StringUtil.isEmptyOrSpaces(workspaceName)) {
      return "Workspace name is empty";
    }

    if (!WorkspaceInfo.isValidName(workspaceName)) {
      return "Workspace name contains invalid symbols";
    }

    return null;
  }

  public String getWorkspaceName() {
    return myForm.getWorkspaceName();
  }

  public String getWorkspaceComment() {
    return myForm.getWorkspaceComment();
  }

  public List<WorkingFolderInfo> getWorkingFolders() {
    return myForm.getWorkingFolders();
  }
}