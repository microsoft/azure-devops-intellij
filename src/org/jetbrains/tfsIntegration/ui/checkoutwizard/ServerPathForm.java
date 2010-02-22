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

import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import java.awt.*;
import java.util.EventListener;

public class ServerPathForm {

  public interface Listener extends EventListener {
    void serverPathChanged();
  }

  private ServerTree myServerTree;
  private JLabel myMessageLabel;
  private JPanel myContentPanel;
  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);
  private final @Nullable ServerTree.PathFilter myPathFilter;

  public ServerPathForm(final ServerTree.PathFilter pathFilter) {
    myPathFilter = pathFilter;
  }

  private void createUIComponents() {
    myServerTree = new ServerTree(true);

    myServerTree.addSelectionListener(new ServerTree.SelectionListener() {
      public void selectionChanged(final ServerTree.SelectedItem selection) {
        myEventDispatcher.getMulticaster().serverPathChanged();
      }
    });
  }

  public void configure(ServerInfo server, String initialPath) {
    myServerTree.configure(server, initialPath, myPathFilter);
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public String getServerPath() {
    final ServerTree.SelectedItem selectedItem = myServerTree.getSelectedItem();
    return selectedItem != null ? selectedItem.path : null;
  }

  public void addListener(Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(Listener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void setErrorMessage(String message) {
    myMessageLabel.setText(message);
    myMessageLabel.setForeground(Color.RED);
  }

  public void setMessage(String message) {
    myMessageLabel.setText(message);
    myMessageLabel.setForeground(Color.BLACK); // TODO
  }

  public JComponent getPreferredFocusedComponent() {
    return myServerTree.getTreeComponent();
  }

}
