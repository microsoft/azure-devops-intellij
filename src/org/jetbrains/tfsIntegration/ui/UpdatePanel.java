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
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.tfsIntegration.core.tfs.AbstractUpdatePanel;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.TfsPanel;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;
import java.util.Collection;

public class UpdatePanel extends AbstractUpdatePanel {
  private JPanel myPanel;
  private JCheckBox myRecursiveBox;
  private JPanel myConfigureWorkspacesPanel;

  public UpdatePanel(Project project, Collection<FilePath> roots) {
    super(project);
    init(roots);
  }

  protected JPanel getRootsPanel() {
    return myConfigureWorkspacesPanel;
  }

  protected TfsPanel createWorkspacePanel(final WorkspaceInfo workspace, final Project project, final Collection<ItemPath> paths) {
    return new UpdateOptionsPanel(workspace, project, paths);
  }

  protected JComponent getPanel() {
    return myPanel;
  }

  protected JCheckBox getRecursiveBox() {
    return myRecursiveBox;
  }

}
