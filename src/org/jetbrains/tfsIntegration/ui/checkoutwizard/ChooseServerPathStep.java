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
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import java.text.MessageFormat;

public class ChooseServerPathStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private final ServerPathForm myPathForm;

  public ChooseServerPathStep(final CheckoutWizardModel model) {
    super("Choose Source Path", model);
    myPathForm = new ServerPathForm(new ServerTree.PathFilter() {
      public boolean isAcceptablePath(final @NotNull String path) {
        return isAcceptable(path);
      }
    });

    myPathForm.addListener(new ServerPathForm.Listener() {
      public void serverPathChanged() {
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
    return isAcceptable(myPathForm.getServerPath());
  }

  public JComponent getComponent() {
    return myPathForm.getContentPanel();
  }

  public void _init() {
    myPathForm.configure(myModel.getServer(), myModel.getServerPath());
    validate();
  }

  public void commit(CommitType commitType) throws CommitStepException {
    if (isAcceptable(myPathForm.getServerPath())) {
      myModel.setServerPath(myPathForm.getServerPath());
    }
  }

  private boolean isAcceptable(String serverPath) {
    if (serverPath == null || serverPath.length() == 0) {
      return false;
    }
    try {
      if (myModel.getWorkspace().findLocalPathByServerPath(serverPath, true) == null) {
        return false;
      }
    }
    catch (TfsException e) {
      return false;
    }
    return true;
  }

  private void validate() {
    String serverPath = myPathForm.getServerPath();
    if (serverPath == null || serverPath.length() == 0) {
      myPathForm.setErrorMessage("Server path is empty");
    }
    else {
      try {
        final FilePath localPath = myModel.getWorkspace().findLocalPathByServerPath(serverPath, true);
        if (localPath != null) {
          String message = MessageFormat.format("Server path ''{0}'' is mapped to ''{1}''", serverPath, localPath.getPresentableUrl());
          myPathForm.setMessage(message);
        }
        else {
          String message = MessageFormat.format("No mapping found for ''{0}''", serverPath);
          myPathForm.setErrorMessage(message);
        }
      }
      catch (TfsException e) {
        myPathForm.setErrorMessage(MessageFormat.format("Failed to connect to server. {0}", e.getMessage()));
      }
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPathForm.getPreferredFocusedComponent();
  }
}