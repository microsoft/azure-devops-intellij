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
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class ResolveConflictsDialog extends DialogWrapper {
  private ResolveConflictsForm myResolveConflictsForm;

  public ResolveConflictsDialog(final Project project,
                                final WorkspaceInfo workspace,
                                final List<ItemPath> paths,
                                final List<Conflict> conflicts,
                                final UpdatedFiles updatedFiles) {
    super(project, true);
    setTitle("Resolve Conflicts");
    setResizable(true);
    myResolveConflictsForm = new ResolveConflictsForm(workspace, project, paths, conflicts, updatedFiles);
    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    JComponent panel = myResolveConflictsForm.getPanel();
    panel.addPropertyChangeListener(ResolveConflictsForm.CLOSE_PROPERTY, new PropertyChangeListener() {
      public void propertyChange(final PropertyChangeEvent evt) {
        ResolveConflictsDialog.this.close(DialogWrapper.OK_EXIT_CODE);
      }
    });
    return panel;
  }

  protected Action[] createActions() {
    setCancelButtonText("Close");
    return new Action[]{getCancelAction()};
  }
}