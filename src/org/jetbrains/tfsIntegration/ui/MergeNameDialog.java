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

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import java.text.MessageFormat;

public class MergeNameDialog extends DialogWrapper {
  private MergeNameForm myMergeNameForm;
  private final WorkspaceInfo myWorkspace;
  private final String myLocalName;
  private final String myServerName;

  public MergeNameDialog(final WorkspaceInfo workspace, String yourName, String theirsName) {
    super(false);
    myWorkspace = workspace;
    myLocalName = yourName;
    myServerName = theirsName;
    setTitle("Merge Changes");
    setResizable(true);
    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myMergeNameForm = new MergeNameForm(myLocalName, myServerName);
    myMergeNameForm.addListener(new MergeNameForm.Listener() {
      public void selectedPathChanged() {
        String errorMessage = validate(myMergeNameForm.getSelectedPath());
        myMergeNameForm.setErrorText(errorMessage);
        getOKAction().setEnabled(errorMessage == null);
      }
    });
    return myMergeNameForm.getPanel();
  }

  @NotNull
  public String getSelectedPath() {
    // it is not null if validated
    //noinspection ConstantConditions
    return myMergeNameForm.getSelectedPath();
  }

  @Nullable
  private String validate(String path) {
    if (path == null || path.length() == 0) {
      return "Path is empty";
    }

    // TODO valid filesystem name?

    try {
      if (!myWorkspace.hasLocalPathForServerPath(path)) {
        return MessageFormat.format("No mapping found for ''{0}'' in workspace ''{1}''", path, myWorkspace.getName());
      }
    }
    catch (TfsException e) {
      Messages.showErrorDialog(e.getMessage(), "Merge");
      close(CANCEL_EXIT_CODE);
    }
    return null;
  }
  
}
