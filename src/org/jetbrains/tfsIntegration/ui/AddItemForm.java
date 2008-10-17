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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserAction;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserForm;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class AddItemForm {

  private final WorkspaceInfo myWorkspace;
  private final String mySourcePath;

  private JPanel myContentPane;

  private ServerBrowserForm myServerBrowserForm;
  private SelectRevisionForm mySelectRevisionForm;

  public AddItemForm(final Project project, final WorkspaceInfo workspace, final String sourcePath) {
    myWorkspace = workspace;
    mySourcePath = sourcePath;

    myServerBrowserForm.addSelectionListener(new ServerTree.SelectionListener() {
      public void selectionChanged(final ServerTree.SelectedItem selection) {
        if (selection != null) {
          mySelectRevisionForm.init(project, workspace, selection.path, selection.isDirectory);
        }
      }
    });

    if (myServerBrowserForm.isItemSelected()) {
      //noinspection ConstantConditions
      mySelectRevisionForm
        .init(project, workspace, myServerBrowserForm.getSelectedPath().path, myServerBrowserForm.getSelectedPath().isDirectory);
    }
  }

  private void createUIComponents() {
    myServerBrowserForm =
      new ServerBrowserForm(false, myWorkspace.getServer(), mySourcePath, null, Collections.<ServerBrowserAction>emptyList());
    myServerBrowserForm.getContentPanel().setPreferredSize(new Dimension(400, 350));
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public void addServerTreeSelectionListener(final ServerTree.SelectionListener listener) {
    myServerBrowserForm.addSelectionListener(listener);
  }

  public void removeServerTreeSelectionListener(final ServerTree.SelectionListener listener) {
    myServerBrowserForm.removeSelectionListener(listener);
  }

  public void addSelectRevisionListener(final SelectRevisionForm.Listener listener) {
    mySelectRevisionForm.addListener(listener);
  }

  public void removeSelectRevisionListener(final SelectRevisionForm.Listener listener) {
    mySelectRevisionForm.removeListener(listener);
  }

  @Nullable
  public ServerTree.SelectedItem getServerItem() {
    return myServerBrowserForm.getSelectedPath();
  }

  @Nullable
  public VersionSpecBase getVersion() {
    return mySelectRevisionForm.getVersionSpec();
  }
  
}
