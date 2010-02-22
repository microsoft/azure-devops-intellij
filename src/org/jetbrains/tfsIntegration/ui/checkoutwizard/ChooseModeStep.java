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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;

public class ChooseModeStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private final CheckoutModeForm myCheckoutModeForm;

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

  @NotNull
  public Object getStepId() {
    return ID;
  }

  @Nullable
  public Object getNextStepId() {
    return ChooseWorkspaceStep.ID;
  }

  @Nullable
  public Object getPreviousStepId() {
    return null;
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
    myCheckoutModeForm.setNewWorkspaceName(myModel.getNewWorkspaceName());
    myCheckoutModeForm.setAutoModeSelected(myModel.getMode() == CheckoutWizardModel.Mode.Auto);

    if (myCheckoutModeForm.isAutoModeSelected()) {
      myCheckoutModeForm.setErrorMessage(validateWorkspaceName(myCheckoutModeForm.getNewWorkspaceName()));
    }
    else {
      myCheckoutModeForm.setErrorMessage(null);
    }
  }

  public void commit(CommitType commitType) throws CommitStepException {
    myModel.setMode(myCheckoutModeForm.isAutoModeSelected() ? CheckoutWizardModel.Mode.Auto : CheckoutWizardModel.Mode.Manual);
    if (validateWorkspaceName(myCheckoutModeForm.getNewWorkspaceName()) == null) {
      myModel.setNewWorkspaceName(myCheckoutModeForm.getNewWorkspaceName());
    }
  }

  public JComponent getComponent() {
    return myCheckoutModeForm.getContentPanel();
  }

  @Nullable
  public String validateWorkspaceName(String name) {
    if (name.length() == 0) {
      return "Workspace name is empty";
    }
    if (!WorkspaceInfo.isValidName(name)) {
      return "Workspace name contains invalid symbols";
    }
    return null;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myCheckoutModeForm.getPreferredFocusedComponent();
  }
}
