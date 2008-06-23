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
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.ResolutionData;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;

import javax.swing.*;
import java.util.Map;

public class ResolveConflictsDialog extends DialogWrapper {
  private ResolveConflictsForm myResolveConflictsForm;

  public ResolveConflictsDialog(final Project project, final Map<Conflict, WorkspaceInfo> conflict2workspace) throws VcsException {
    super(project, true);
    setTitle("Resolve Conflicts");
    setResizable(true);
    myResolveConflictsForm = new ResolveConflictsForm(conflict2workspace, project);
    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myResolveConflictsForm.getPanel();
  }

  public Map<Conflict, ResolutionData> getMergeResult() {
    return myResolveConflictsForm.getMergeResult();
  }

  protected Action[] createActions() {
    setCancelButtonText("Close");
    return new Action[]{ getCancelAction() };
  }
}