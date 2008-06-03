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

import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ServerPathForm {

  public interface Listener {
    void serverPathChanged();
  }

  private ServerTree myServerTree;
  private JLabel myTitleLabel;
  private JLabel myMessageLabel;
  private JPanel myContentPanel;

  private List<Listener> myListeners = new ArrayList<Listener>();

  private void createUIComponents() {
    myServerTree = new ServerTree(true);

    myServerTree.addSelectionListener(new ServerTree.SelectionListener() {
      public void selectionChanged(final String selection) {
        fireServerPathChanged();
      }
    });
  }

  public void setPathFilter(final @Nullable ServerTree.PathFilter pathFilter) {
    myServerTree.setPathFilter(pathFilter);
  }

  public void setServer(ServerInfo server) {
    myServerTree.setServer(server);
    String labelText = MessageFormat.format("Source path at {0}", server.getUri());
    myTitleLabel.setText(labelText);
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public String getServerPath() {
    return myServerTree.getSelectedPath();
  }

  public void setServerPath(final String serverPath) {
    myServerTree.setSelectedPath(serverPath, true);
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.remove(listener);
  }

  public void setErrorMessage(String message) {
    myMessageLabel.setText(message);
    myMessageLabel.setForeground(Color.RED);
  }

  public void setMessage(String message) {
    myMessageLabel.setText(message);
    myMessageLabel.setForeground(Color.BLACK); // TODO
  }

  private void fireServerPathChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.serverPathChanged();
    }
  }

}
