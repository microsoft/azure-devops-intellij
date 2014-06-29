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
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.List;

public class WorkspaceDialog extends DialogWrapper {

  private final WorkspaceForm myForm;

  public WorkspaceDialog(Project project, @NotNull ServerInfo server) {
    super(project, true);
    myForm = new WorkspaceForm(project, server);
    myForm.addListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        revalidate();
      }
    });

    init();

    setTitle(TFSBundle.message("create.workspace.dialog.title"));
    setOKButtonText(TFSBundle.message("create.workspace.dialog.ok.button.text"));
    revalidate();
  }

  public WorkspaceDialog(Project project, @NotNull WorkspaceInfo workspace) {
    super(project, true);

    myForm = new WorkspaceForm(project, workspace);
    myForm.addListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        revalidate();
      }
    });

    init();

    setTitle(TFSBundle.message("edit.workspace.dialog.title"));
    setOKButtonText(TFSBundle.message("create.workspace.dialog.ok.button.text"));

    revalidate();
  }

  private void revalidate() {
    String errorMessage = getErrorMessage();
    setOKActionEnabled(errorMessage == null);
    myForm.setErrorMessage(errorMessage);
  }

  private String getErrorMessage() {
    String message = validate(myForm.getWorkspaceName());
    if (message == null) {
      message = myForm.validateWorkingFolders();
    }
    return message;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ManageWorkspace";
  }

  protected JComponent createCenterPanel() {
    return myForm.getContentPane();
  }

  @Nullable
  private static String validate(String workspaceName) {
    if (StringUtil.isEmptyOrSpaces(workspaceName)) {
      return TFSBundle.message("workspace.name.empty");
    }

    if (!WorkspaceInfo.isValidName(workspaceName)) {
      return TFSBundle.message("workspace.name.invalid");
    }

    return null;
  }

  public String getWorkspaceName() {
    return myForm.getWorkspaceName();
  }

  @NotNull
  public WorkspaceInfo.Location getWorkspaceLocation() {
    return myForm.getWorkspaceLocation();
  }

  public String getWorkspaceComment() {
    return myForm.getWorkspaceComment();
  }

  public List<WorkingFolderInfo> getWorkingFolders() {
    return myForm.getWorkingFolders();
  }

  @Override
  protected String getHelpId() {
    return "project.propVCSSupport.VCSs.TFS.manage.connect.createWorkspace";
  }
}
