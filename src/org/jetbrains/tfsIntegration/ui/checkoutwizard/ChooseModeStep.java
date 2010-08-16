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
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ChooseModeStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private final CheckoutModeForm myForm;

  public ChooseModeStep(final CheckoutWizardModel model) {
    super("Checkout Mode", model);
    myForm = new CheckoutModeForm();
    myForm.addListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        revalidate();
        fireStateChanged();
      }
    });
    revalidate();
  }

  private void revalidate() {
    myForm.setErrorMessage(validate());
  }

  @Nullable
  private String validate() {
    if (myForm.isAutoModeSelected()) {
      String name = myForm.getNewWorkspaceName();
      if (StringUtil.isEmpty(name)) {
        return TFSBundle.message("workspace.name.empty");
      }
      if (!WorkspaceInfo.isValidName(name)) {
        return TFSBundle.message("workspace.name.invalid");
      }
    }
    return null;
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
    return validate() == null;
  }

  public void _init() {
    myForm.setNewWorkspaceName(myModel.getNewWorkspaceName());
    myForm.setAutoModeSelected(myModel.getMode() == CheckoutWizardModel.Mode.Auto);
    revalidate();
  }

  public void commit(CommitType commitType) throws CommitStepException {
    myModel.setMode(myForm.isAutoModeSelected() ? CheckoutWizardModel.Mode.Auto : CheckoutWizardModel.Mode.Manual);
    myModel.setNewWorkspaceName(myForm.getNewWorkspaceName());
  }

  public JComponent getComponent() {
    return myForm.getContentPanel();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }
}
