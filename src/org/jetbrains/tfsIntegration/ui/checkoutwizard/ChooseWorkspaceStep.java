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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.UserCancelledException;
import org.jetbrains.tfsIntegration.ui.ManageWorkspacesForm;
import org.jetbrains.tfsIntegration.ui.abstractwizard.CommitStepCancelledException;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.List;

public class ChooseWorkspaceStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private final ManageWorkspacesForm myManageWorkspacesForm;

  public ChooseWorkspaceStep(Project project, final CheckoutWizardModel model) {
    super("Source Workspace", model);
    myManageWorkspacesForm = new ManageWorkspacesForm(project);
    myManageWorkspacesForm.addSelectionListener(new ManageWorkspacesForm.Listener() {
      public void selectionChanged() {
        fireStateChanged();
      }
    });
  }

  @NotNull
  public Object getStepId() {
    return ID;
  }

  public Object getNextStepId() {
    if (myModel.getMode() == CheckoutWizardModel.Mode.Manual) {
      return ChooseServerPathStep.ID;
    }
    else {
      return ChooseLocalAndServerPathsStep.ID;
    }
  }

  public Object getPreviousStepId() {
    return ChooseModeStep.ID;
  }

  public boolean isComplete() {
    if (myModel.getMode() == CheckoutWizardModel.Mode.Manual) {
      return myManageWorkspacesForm.getSelectedWorkspace() != null;
    }
    else {
      return myManageWorkspacesForm.getSelectedServer() != null;
    }
  }

  public void _init() {
    if (myModel.getMode() == CheckoutWizardModel.Mode.Manual) {
      setTitle("Source Workspace");
      myManageWorkspacesForm.setShowWorkspaces(true);
      myManageWorkspacesForm.setSelectedWorkspace(myModel.getWorkspace());
    }
    else {
      setTitle("Source Server");
      myManageWorkspacesForm.setShowWorkspaces(false);
      myManageWorkspacesForm.setSelectedServer(myModel.getServer());
    }
  }

  public void commit(CommitType commitType) throws CommitStepException {
    if (myModel.getMode() == CheckoutWizardModel.Mode.Manual) {
      final WorkspaceInfo workspace = myManageWorkspacesForm.getSelectedWorkspace();
      if (workspace != null) {
        myModel.setServer(workspace.getServer());
      }
      myModel.setWorkspace(workspace);
      if (commitType == CommitType.Next || commitType == CommitType.Finish) {
        // let's select first mapped path for newly selected workspace
        try {
          // workspace can't be null here
          //noinspection ConstantConditions
          final List<WorkingFolderInfo> workingFolders = workspace.getWorkingFolders();
          if (!workingFolders.isEmpty()) {
            myModel.setServerPath(workingFolders.get(0).getServerPath());
          }
          else {
            String message = MessageFormat.format("Workspace ''{0}'' has no mappings.", workspace.getName());
            throw new CommitStepException(message);
          }
        }
        catch (UserCancelledException e) {
          throw new CommitStepCancelledException();
        }
        catch (TfsException e) {
          throw new CommitStepException(e.getMessage());
        }
      }
    }
    else {
      final ServerInfo server = myManageWorkspacesForm.getSelectedServer();
      myModel.setServer(server);
      if (commitType == CommitType.Next || commitType == CommitType.Finish) {
        //noinspection ConstantConditions
        try {
          WebServiceHelper.authenticate(server.getUri());
        }
        catch (UserCancelledException e) {
          throw new CommitStepCancelledException();
        }
        catch (TfsException e) {
          throw new CommitStepException(e.getMessage());
        }
      }
    }
  }

  public JComponent getComponent() {
    return myManageWorkspacesForm.getContentPane();
  }

  public boolean showWaitCursorOnCommit() {
    return true;
  }
}
