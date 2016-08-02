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
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.servertree.TfsTreeForm;

import javax.swing.*;

public class ChooseServerPathStep extends CheckoutWizardStep {

  public static final Object ID = new Object();

  private final TfsTreeForm myForm = new TfsTreeForm();

  public ChooseServerPathStep(final CheckoutWizardModel model) {
    super("Choose Source Path", model);

    Disposer.register(this, myForm);
    myForm.addListener(new TfsTreeForm.SelectionListener() {
      @Override
      public void selectionChanged() {
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
    return isAcceptable(myForm.getSelectedPath());
  }

  public JComponent getComponent() {
    return myForm.getContentPane();
  }

  public void _init() {
    myForm.initialize(myModel.getServer(), myModel.getServerPath(), true, false, new Condition<String>() {
      @Override
      public boolean value(String path) {
        return isAcceptable(path);
      }
    });
    validate();
  }

  public void commit(CommitType commitType) throws CommitStepException {
    if (isAcceptable(myForm.getSelectedPath())) {
      myModel.setServerPath(myForm.getSelectedPath());
    }
  }

  private boolean isAcceptable(String serverPath) {
    if (StringUtil.isEmpty(serverPath)) {
      return false;
    }
    try {
      if (myModel.getWorkspace().findLocalPathByServerPath(serverPath, true, null) == null) {
        return false;
      }
    }
    catch (TfsException e) {
      return false;
    }
    return true;
  }

  private void validate() {
    String serverPath = myForm.getSelectedPath();
    if (StringUtil.isEmpty(serverPath)) {
      myForm.setMessage(TFSBundle.message("server.path.is.not.selected"), true);
    }
    else {
      try {
        final FilePath localPath = myModel.getWorkspace().findLocalPathByServerPath(serverPath, true, null);
        if (localPath != null) {
          myForm.setMessage(TFSBundle.message("server.path.0.is.mapped.to.1", serverPath, localPath.getPresentableUrl()), false);
        }
        else {
          myForm.setMessage(TFSBundle.message("no.mapping.for.0", serverPath), true);
        }
      }
      catch (TfsException e) {
        myForm.setMessage(TFSBundle.message("failed.to.connect", e.getMessage()), true);
      }
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }

  public String getHelpId() {
    return "reference.checkoutTFS.sourcepath";
  }
}