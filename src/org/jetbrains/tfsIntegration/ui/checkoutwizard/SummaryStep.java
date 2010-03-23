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
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;

public class SummaryStep extends CheckoutWizardStep {

  public static final Object ID = new Object();
  private final SummaryForm mySummaryForm;

  public SummaryStep(final CheckoutWizardModel model) {
    super("Summary", model);

    mySummaryForm = new SummaryForm();
  }

  @NotNull
  public Object getStepId() {
    return ID;
  }

  @Nullable
  public Object getNextStepId() {
    return null;
  }

  @Nullable
  public Object getPreviousStepId() {
    if (myModel.getMode() == CheckoutWizardModel.Mode.Manual) {
      return ChooseServerPathStep.ID;
    }
    else {
      return ChooseLocalAndServerPathsStep.ID;
    }
  }

  public boolean isComplete() {
    return true;
  }

  public void commit(final CommitType commitType) throws CommitStepException {
    // nothing here
  }

  public JComponent getComponent() {
    return mySummaryForm.getContentPanel();
  }

  public void _init() {
    mySummaryForm.setServer(myModel.getServer());
    mySummaryForm.setServerPath(myModel.getServerPath());

    if (myModel.getMode() == CheckoutWizardModel.Mode.Auto) {
      mySummaryForm.setNewWorkspaceName(myModel.getNewWorkspaceName());
      mySummaryForm.setLocalPath(myModel.getDestinationFolder());
    }
    else {
      mySummaryForm.setWorkspace(myModel.getWorkspace());
      try {
        final FilePath localPath = myModel.getWorkspace().findLocalPathByServerPath(myModel.getServerPath(), true);
        mySummaryForm.setLocalPath(localPath.getPresentableUrl());
      }
      catch (TfsException e) {
        // should not happen since workspace should be loaded
        TFSVcs.error(e.getMessage());
      }
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }
}
