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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.vcsUtil.VcsUtil;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

public class LocalAndServerPathsForm {

  public interface Listener extends EventListener {
    void serverPathChanged();

    void localPathChanged();
  }

  private ServerTree myServerTree;
  private JTextField myLocalPathField;
  private JButton myBrowseButton;
  private JPanel myContentPanel;
  private JLabel myErrorLabel;

  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  public LocalAndServerPathsForm() {
    myServerTree.addSelectionListener(new ServerTree.SelectionListener() {
      public void selectionChanged(final ServerTree.SelectedItem selection) {
        myEventDispatcher.getMulticaster().serverPathChanged();
      }
    });

    myLocalPathField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myEventDispatcher.getMulticaster().localPathChanged();
      }
    });

    myBrowseButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        FileChooserDescriptor d = new FileChooserDescriptor(false, true, false, false, false, false);
        d.setTitle("Choose Local Folder");
        d.setShowFileSystemRoots(true);
        d.setDescription("Choose local folder to check out to");

        VirtualFile root = myLocalPathField.getText() != null ? VcsUtil.getVirtualFile(myLocalPathField.getText().trim()) : null;
        VirtualFile[] files = FileChooser.chooseFiles(myContentPanel, d, root);
        if (files.length == 1 && files[0] != null) {
          myLocalPathField.setText(files[0].getPresentableUrl());
          myLocalPathField.setText(files[0].getPresentableUrl());
        }
      }
    });
  }

  private void createUIComponents() {
    myServerTree = new ServerTree(true);
  }

  public void configure(ServerInfo server, String initialPath) {
    myServerTree.configure(server, initialPath, null);
  }

  public String getLocalPath() {
    return myLocalPathField.getText();
  }

  public void setLocalPath(String path) {
    myLocalPathField.setText(path);
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
    myErrorLabel.setText(message);
  }

  public JComponent getPreferredFocusedComponent() {
    return myServerTree.getTreeComponent();
  }

}
