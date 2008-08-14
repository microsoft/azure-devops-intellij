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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;

import javax.swing.*;

public class CreateBranchDialog extends DialogWrapper {
  private CreateBranchForm myForm;
  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final String myServerPath;

  public CreateBranchDialog(final Project project, final WorkspaceInfo workspace, final String serverPath) {
    super(project, true);
    myProject = project;
    myWorkspace = workspace;
    myServerPath = serverPath;

    setTitle("Create Branch");
    setResizable(true);
    init();

    setOKActionEnabled(true);
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myForm = new CreateBranchForm(myProject, myWorkspace, myServerPath);
    return myForm.getPanel();
  }

  @Nullable
  public VersionSpecBase getVersionSpec() {
    return myForm.getVersionSpec();
  }

  public String getTargetPath() {
    return myForm.getTargetPath();
  }

  public boolean isCreateWorkingCopies() {
    return myForm.isCreateWorkingCopies();
  }
}