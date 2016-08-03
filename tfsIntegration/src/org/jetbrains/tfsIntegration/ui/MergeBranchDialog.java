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

public class MergeBranchDialog extends DialogWrapper {
  private final String mySourcePath;
  private final boolean mySourceIsDirectory;
  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private MergeBranchForm myMergeBranchForm;

  public MergeBranchDialog(Project project,
                           final WorkspaceInfo workspace,
                           final String sourcePath,
                           final boolean sourceIsDirectory,
                           String title) {
    super(project, true);
    myProject = project;
    myWorkspace = workspace;
    mySourcePath = sourcePath;
    mySourceIsDirectory = sourceIsDirectory;

    setTitle(title);
    setResizable(true);
    init();
  }

  public String getSourcePath() {
    return myMergeBranchForm.getSourcePath();
  }

  public String getTargetPath() {
    return myMergeBranchForm.getTargetPath();

  }

  @Nullable
  public VersionSpecBase getFromVersion() {
    return myMergeBranchForm.getFromVersion();
  }

  @Nullable
  public VersionSpecBase getToVersion() {
    return myMergeBranchForm.getToVersion();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myMergeBranchForm = new MergeBranchForm(myProject, myWorkspace, mySourcePath, mySourceIsDirectory, getTitle());

    myMergeBranchForm.addListener(new MergeBranchForm.Listener() {
      public void stateChanged(final boolean canFinish) {
        setOKActionEnabled(canFinish);
      }
    });

    return myMergeBranchForm.getContentPanel();
  }

  protected void doOKAction() {
    myMergeBranchForm.close();
    super.doOKAction();
  }

  public void doCancelAction() {
    myMergeBranchForm.close();
    super.doCancelAction();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.MergeBranch";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMergeBranchForm.getPreferredFocusedComponent();
  }
}
