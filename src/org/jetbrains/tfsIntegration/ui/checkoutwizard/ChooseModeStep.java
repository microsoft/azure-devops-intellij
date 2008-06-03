package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import com.intellij.ide.wizard.CommitStepException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;

public class ChooseModeStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private CheckoutModeForm myCheckoutModeForm;

  public ChooseModeStep(final CheckoutWizardModel model) {
    super("Checkout Mode", model);
    myCheckoutModeForm = new CheckoutModeForm();
    myCheckoutModeForm.addListener(new CheckoutModeForm.Listener() {

      public void modeChanged(final boolean autoModeSelected) {
        if (myCheckoutModeForm.isAutoModeSelected()) {
          myCheckoutModeForm.setErrorMessage(validateWorkspaceName(myCheckoutModeForm.getNewWorkspaceName()));
        }
        else {
          myCheckoutModeForm.setErrorMessage(null);
        }
        fireStateChanged();
      }

      public void newWorkspaceNameChanged(final String workspaceName) {
        if (myCheckoutModeForm.isAutoModeSelected()) {
          myCheckoutModeForm.setErrorMessage(validateWorkspaceName(workspaceName));
        }
        fireStateChanged();
      }
    });
  }

  public Object getStepId() {
    return ID;
  }

  @Nullable
  public Object getNextStepId() {
    return myCheckoutModeForm.isAutoModeSelected() ? ChooseLocalAndServerPathsStep.ID : ChooseWorkspaceStep.ID;
  }

  @Nullable
  public Object getPreviousStepId() {
    return ChooseServerStep.ID;
  }

  public boolean isComplete() {
    if (myCheckoutModeForm.isAutoModeSelected()) {
      return validateWorkspaceName(myCheckoutModeForm.getNewWorkspaceName()) == null;

    }
    else {
      return true;
    }
  }

  public void _init() {
    myCheckoutModeForm.setAutoModeSelected(myModel.getMode() == CheckoutWizardModel.Mode.Auto);
    myCheckoutModeForm.setNewWorkspaceName(myModel.getNewWorkspaceName());

    if (myCheckoutModeForm.isAutoModeSelected()) {
      myCheckoutModeForm.setErrorMessage(validateWorkspaceName(myCheckoutModeForm.getNewWorkspaceName()));
    }
    else {
      myCheckoutModeForm.setErrorMessage(null);
    }
  }

  public void _commit(final boolean finishChosen) throws CommitStepException {
    myModel.setMode(myCheckoutModeForm.isAutoModeSelected() ? CheckoutWizardModel.Mode.Auto : CheckoutWizardModel.Mode.Manual);
    if (validateWorkspaceName(myCheckoutModeForm.getNewWorkspaceName()) == null) {
      myModel.setNewWorkspaceName(myCheckoutModeForm.getNewWorkspaceName());
    }
  }

  public JComponent getComponent() {
    return myCheckoutModeForm.getContentPanel();
  }

  public boolean showWaitCursorOnCommit() {
    return false;
  }

  public String validateWorkspaceName(String name) {
    if (name.length() == 0) {
      return "Workspace name is empty";
    }
    if (!WorkspaceInfo.isValidName(name)) {
      return "Workspace name contains invalid symbols";
    }

    for (WorkspaceInfo existingWorkspace : myModel.getServer().getWorkspacesForCurrentOwner()) {
      if (existingWorkspace.getName().equalsIgnoreCase(name)) {
        return "Workspace with a given name already exists";
      }
    }

    return null;
  }

}