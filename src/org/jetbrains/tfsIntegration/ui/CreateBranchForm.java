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
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserDialog;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateBranchForm {
  private JTextField mySourceField;
  private SelectRevisionForm myRevisionForm;
  private JCheckBox myCreateLocalWorkingCopiesCheckBox;
  private TextFieldWithBrowseButton.NoPathCompletion myTargetField;
  private JPanel myContentPane;
  private JLabel myTargetLabel;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public CreateBranchForm(final Project project,
                          final WorkspaceInfo workspace,
                          String serverPath,
                          boolean isDirectory) {
    mySourceField.setText(serverPath);

    myTargetLabel.setLabelFor(myTargetField.getChildComponent());
    myTargetField.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        String serverPath =
          myTargetField.getText() != null && myTargetField.getText().length() > 0 ? myTargetField.getText() : mySourceField.getText();
        ServerBrowserDialog d =
          new ServerBrowserDialog(TFSBundle.message("choose.branch.target.folder.dialog.title"), project, workspace.getServer(),
                                  serverPath, true, true);
        if (d.showAndGet()) {
          myTargetField.setText(d.getSelectedPath());
        }
      }
    });

    myTargetField.getChildComponent().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(e));
      }
    });

    myRevisionForm.init(project, workspace, serverPath, isDirectory);
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  @Nullable
  public VersionSpecBase getVersionSpec() {
    return myRevisionForm.getVersionSpec();
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  public String getTargetPath() {
    return myTargetField.getText();
  }

  public boolean isCreateWorkingCopies() {
    return myCreateLocalWorkingCopiesCheckBox.isSelected();
  }

  public JComponent getPreferredFocusedComponent() {
    return myTargetField.getChildComponent();
  }
}
