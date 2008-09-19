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

import javax.swing.*;


public class SelectChangesetDialog extends DialogWrapper {
  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private SelectChangesetForm myForm;
  private final String myServerPath;
  private final boolean myRecursive;

  public SelectChangesetDialog(final Project project, final WorkspaceInfo workspace, final String serverPath, final boolean recursive) {
    super(project, true);
    myProject = project;
    myWorkspace = workspace;
    myServerPath = serverPath;
    myRecursive = recursive;

    setOKButtonText("Choose");
    setTitle("Find Changeset");
    setResizable(true);
    init();

    setOKActionEnabled(false);
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myForm = new SelectChangesetForm(myProject, myWorkspace, myServerPath, myRecursive);

    myForm.addListener(new SelectChangesetForm.Listener() {
      public void selectionChanged(final Integer changeset) {
        setOKActionEnabled(changeset != null);
      }
    });

    return myForm.getPanel();
  }

  @Nullable
  public Integer getChangeset() {
    return myForm.getChangeset();
  }
}