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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;
import java.util.Map;

public class SelectWorkItemsDialog extends DialogWrapper {
  private final Map<ServerInfo, WorkItemsDialogState> myState;

  public SelectWorkItemsDialog(final Project project,
                               final Map<ServerInfo, WorkItemsDialogState> state) {
    super(project, false);
    myState = state;
    setTitle("Work Items");
    init();
  }

  protected JComponent createCenterPanel() {
    final SelectWorkItemsForm form = new SelectWorkItemsForm(myState, getTitle(), getContentPane());
    return form.getContentPane();
  }

}

