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
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.VersionControlLabel;

import javax.swing.*;


// TODO: disable Ok button is label's table is empty  
public class SelectLabelDialog extends DialogWrapper {
  private WorkspaceInfo myWorkspace;
  private TFSVcs myVcs;
  private SelectLabelPanel labelPanel;

  public SelectLabelDialog(final Project project, final WorkspaceInfo workspace) {
    super(project, true);
    myWorkspace = workspace;
    setOKButtonText("Choose");
    setTitle("Choose Label");

    setResizable(true);

    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    labelPanel = new SelectLabelPanel(myWorkspace);
    return labelPanel.getPanel();
  }

  String getLabelString() {
    VersionControlLabel label = labelPanel.getLabel();
    if (label == null) {
      return null;
    }
    return label.getName() + "@" + label.getScope();
  }
}
