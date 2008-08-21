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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;

import javax.swing.*;
import java.util.Collection;

public class MergeBranchDialog extends DialogWrapper {
  private final String mySourceServerPath;
  private final Collection<Item> myTargetBranches;
  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private MergeBranchForm myMergeBranchForm;

  public MergeBranchDialog(Project project,
                           final WorkspaceInfo workspace,
                           final String sourceServerPath,
                           final Collection<Item> targetBranches,
                           String title) {
    super(project, false);
    myProject = project;
    myWorkspace = workspace;
    mySourceServerPath = sourceServerPath;
    myTargetBranches = targetBranches;

    setTitle(title);
    setResizable(true);
    init();
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
    myMergeBranchForm = new MergeBranchForm(myProject, myWorkspace, mySourceServerPath, myTargetBranches);

    myMergeBranchForm.addListener(new MergeBranchForm.Listener() {
      public void stateChanged(final boolean canFinish) {
        setOKActionEnabled(canFinish);
      }
    });

    return myMergeBranchForm.getContentPanel();
  }

}
