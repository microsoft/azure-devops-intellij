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
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CreateBranchDialog extends DialogWrapper {
  private final CreateBranchForm myForm;

  public CreateBranchDialog(final Project project, final WorkspaceInfo workspace, final String serverPath, final boolean isDirectory) {
    super(project, true);
    myForm = new CreateBranchForm(project, workspace, serverPath, isDirectory);
    myForm.addListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        revalidate();
      }
    });

    setTitle(TFSBundle.message("create.branch.dialog.title"));
    setSize(380, 450);

    init();
    revalidate();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myForm.getContentPane();
  }

  private void revalidate() {
    setOKActionEnabled(StringUtil.isNotEmpty(myForm.getTargetPath()));
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

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.CreateBranch";
  }

}