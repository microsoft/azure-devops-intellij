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

package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;

public class ServerBrowserForm {

  private JLabel mySelectedPathLabel;
  private ServerTree myTree;
  private JPanel myContentPanel;
  private JPanel myToolbarPanel;

  private final boolean myFoldersOnly;
  private final @NotNull ServerInfo myServer;
  private final @Nullable String myInitialPath;
  private final @Nullable ServerTree.PathFilter myPathFilter;
  private final Collection<? extends ServerBrowserAction> myActions;

  public ServerBrowserForm(final boolean foldersOnly,
                           @NotNull final ServerInfo server,
                           final @Nullable String initialPath,
                           final @Nullable ServerTree.PathFilter pathFilter,
                           Collection<? extends ServerBrowserAction> actions) {
    myFoldersOnly = foldersOnly;
    myServer = server;
    myInitialPath = initialPath;
    myPathFilter = pathFilter;
    myActions = actions;

    updateControls(myTree.getSelectedItem());
  }

  private void createUIComponents() {
    myTree = new ServerTree(myFoldersOnly);
    myTree.configure(myServer, myInitialPath, myPathFilter);

    myTree.addSelectionListener(new ServerTree.SelectionListener() {
      public void selectionChanged(final ServerTree.SelectedItem selection) {
        updateControls(selection);
      }
    });

    myToolbarPanel = new JPanel(new BorderLayout());
    if (!myActions.isEmpty()) {
      DefaultActionGroup actionGroup = new DefaultActionGroup();
      for (ServerBrowserAction action : myActions) {
        action.setServerTree(myTree);
        actionGroup.add(action);
      }
      final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("unknown", actionGroup, true);
      myToolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
      updateActions();
    }
  }

  private void updateControls(final ServerTree.SelectedItem selection) {
    String text = MessageFormat.format("Selected path: {0}", selection != null ? selection.path : "none");
    mySelectedPathLabel.setText(text);
    updateActions();
  }

  private void updateActions() {
    for (ServerBrowserAction action : myActions) {
      AnActionEvent event = new AnActionEvent(null, DataManager.getInstance().getDataContext(myTree.getContentPanel()),
                                              ActionPlaces.UNKNOWN, action.getTemplatePresentation(), ActionManager.getInstance(), 0);
      action.update(event);
    }
  }

  public JComponent getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public ServerTree.SelectedItem getSelectedPath() {
    return myTree.getSelectedItem();
  }
}
