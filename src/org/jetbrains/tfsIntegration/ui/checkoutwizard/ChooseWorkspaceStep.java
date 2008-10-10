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

package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.project.Project;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.ManageWorkspacesForm;

import javax.swing.*;
import java.util.List;

public class ChooseWorkspaceStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private ManageWorkspacesForm myManageWorkspacesForm;

  public ChooseWorkspaceStep(Project project, final CheckoutWizardModel model) {
    super("Source Workspace", model);
    myManageWorkspacesForm = new ManageWorkspacesForm(project, ManageWorkspacesForm.Mode.Choose);
    myManageWorkspacesForm.addSelectionListener(new ManageWorkspacesForm.Listener() {
      public void selectionChanged(final WorkspaceInfo selection) {
        fireStateChanged();
      }

      public void chosen(final WorkspaceInfo selection) {
        fireGoNext();
      }
    });
  }

  public Object getStepId() {
    return ID;
  }

  public Object getNextStepId() {
    return ChooseServerPathStep.ID;
  }

  public Object getPreviousStepId() {
    return ChooseModeStep.ID;
  }

  public boolean isComplete() {
    return myManageWorkspacesForm.getSelectedWorkspace() != null;
  }

  public void _init() {
    myManageWorkspacesForm.setServer(myModel.getServer());
    myManageWorkspacesForm.setSelectedWorkspace(myModel.getWorkspace());
  }

  public void _commit(final boolean finishChosen) throws CommitStepException {
    final WorkspaceInfo newWorkspace = myManageWorkspacesForm.getSelectedWorkspace();

    // let's select first mapped path for newly selected workspace 
    //noinspection ConstantConditions
    if (!newWorkspace.equals(myModel.getWorkspace())) {
      try {
        //noinspection ConstantConditions
        final List<WorkingFolderInfo> workingFolders = newWorkspace.getWorkingFolders();
        if (!workingFolders.isEmpty()) {
          myModel.setServerPath(workingFolders.get(0).getServerPath());
        }
      }
      catch (TfsException e) {
        throw new CommitStepException(e.getMessage());
      }
    }
    myModel.setWorkspace(myManageWorkspacesForm.getSelectedWorkspace());
  }

  public JComponent getComponent() {
    return myManageWorkspacesForm.getContentPane();
  }

  public boolean showWaitCursorOnCommit() {
    return false;
  }
}