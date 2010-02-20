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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ChooseLocalAndServerPathsStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private final LocalAndServerPathsForm myPathsForm;

  public ChooseLocalAndServerPathsStep(final CheckoutWizardModel model) {
    super("Choose Source and Destination Paths", model);
    myPathsForm = new LocalAndServerPathsForm();
    myPathsForm.addListener(new LocalAndServerPathsForm.Listener() {
      public void serverPathChanged() {
        validate();
        fireStateChanged();
      }

      public void localPathChanged() {
        validate();
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
    return SummaryStep.ID;
  }

  @Nullable
  public Object getPreviousStepId() {
    return ChooseWorkspaceStep.ID;
  }

  public boolean isComplete() {
    if (validateLocalPath(myPathsForm.getLocalPath()) != null) {
      return false;
    }
    if (validateServerPath(myPathsForm.getServerPath()) != null) {
      return false;
    }
    return true;
  }

  public JComponent getComponent() {
    return myPathsForm.getContentPanel();
  }

  public void _init() {
    myPathsForm.configure(myModel.getServer(), myModel.getServerPath());
    validate();
  }

  public void commit(CommitType commitType) throws CommitStepException {
    if (validateLocalPath(myPathsForm.getLocalPath()) == null) {
      myModel.setDestinationFolder(myPathsForm.getLocalPath());
    }

    if (validateServerPath(myPathsForm.getServerPath()) == null) {
      myModel.setServerPath(myPathsForm.getServerPath());
    }
  }

  @Nullable
  private static String validateLocalPath(String path) {
    if (path == null || path.length() == 0) {
      return "Local path not specified";
    }
    VirtualFile file = VcsUtil.getVirtualFile(path);
    if (file != null && file.exists() && !file.isDirectory()) {
      return "Destination path represents a file, not a folder";
    }
    return null;
  }

  @Nullable
  private static String validateServerPath(String path) {
    if (path == null || path.length() == 0) {
      return "Server path is empty";
    }
    return null;
  }

  private void validate() {
    String errorMessage = validateServerPath(myPathsForm.getServerPath());
    if (errorMessage == null) {
      errorMessage = validateLocalPath(myPathsForm.getLocalPath());
    }
    myPathsForm.setErrorMessage(errorMessage);
  }
}
