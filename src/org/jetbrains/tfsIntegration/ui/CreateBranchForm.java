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
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateBranchForm {
  private JTextField mySourceField;
  private SelectRevisionForm myRevisionForm;
  private JCheckBox myCreateLocalWorkingCopiesCheckBox;
  private TextFieldWithBrowseButton myTargetField;
  private JPanel myPanel;

  public CreateBranchForm(final Project project, final WorkspaceInfo workspace, ItemPath path) {
    mySourceField.setText(path.getServerPath());
    
    myTargetField.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        ServerBrowserDialog d =
          new ServerBrowserDialog("Choose Target Folder to Create Branch", project, workspace.getServer(), myTargetField.getText(), true);
        d.show();
        if (d.isOK()) {
          myTargetField.setText(d.getSelectedPath());
        }
      }
    });

    myRevisionForm.init(project, workspace, path);
  }

  @Nullable
  public VersionSpecBase getVersionSpec() {
    return myRevisionForm.getVersionSpec();
  }

  public JComponent getPanel() {
    return myPanel;
  }

  public String getTargetPath() {
    return myTargetField.getText();
  }

  public boolean isCreateWorkingCopies() {
    return myCreateLocalWorkingCopiesCheckBox.isSelected();
  }
}
