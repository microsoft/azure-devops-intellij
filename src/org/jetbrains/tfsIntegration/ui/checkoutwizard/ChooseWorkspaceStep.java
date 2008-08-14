package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.project.Project;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.ui.ManageWorkspacesForm;

import javax.swing.*;

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
    myModel.setWorkspace(myManageWorkspacesForm.getSelectedWorkspace());
  }

  public JComponent getComponent() {
    return myManageWorkspacesForm.getContentPane();
  }

  public boolean showWaitCursorOnCommit() {
    return false;
  }
}