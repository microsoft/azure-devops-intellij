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
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.BranchRelative;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

import javax.swing.*;

public class ItemInfoDialog extends DialogWrapper {

  private final ExtendedItem myExtendedItem;
  private final WorkspaceInfo myWorkspace;
  private final BranchRelative[] myBranches;

  public ItemInfoDialog(final Project project,
                        final WorkspaceInfo workspace,
                        final ExtendedItem extendedItem,
                        final BranchRelative[] branches, String title) {
    super(project, false);
    myWorkspace = workspace;
    myBranches = branches;
    myExtendedItem = extendedItem;

    setTitle(title);
    setResizable(true);
    init();
    setOKButtonText("Close");
  }

  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }

  @Nullable
  protected JComponent createCenterPanel() {
    ItemInfoForm itemInfoForm = new ItemInfoForm(myWorkspace, myExtendedItem, myBranches);
    return itemInfoForm.getPanel();
  }
}
