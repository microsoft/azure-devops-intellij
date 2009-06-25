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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.VersionControlLabel;

import javax.swing.*;

public class SelectLabelDialog extends DialogWrapper {

  private final WorkspaceInfo myWorkspace;
  private SelectLabelForm mySelectLabelForm;

  public SelectLabelDialog(final Project project, final WorkspaceInfo workspace) {
    super(project, true);
    myWorkspace = workspace;
    setTitle("Choose Label");
    setOKButtonText("Choose");

    init();
    updateButtons();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    mySelectLabelForm = new SelectLabelForm(this, myWorkspace);
    mySelectLabelForm.addListener(new SelectLabelForm.Listener() {
      public void selectionChanged() {
        updateButtons();
      }
    });
    return mySelectLabelForm.getContentPane();
  }

  private void updateButtons() {
    setOKActionEnabled(mySelectLabelForm.isLabelSelected());
  }

  @NotNull
  public String getLabelString() {
    VersionControlLabel label = mySelectLabelForm.getLabel();
    return label.getName() + "@" + label.getScope();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.SelectLabel";
  }

}
