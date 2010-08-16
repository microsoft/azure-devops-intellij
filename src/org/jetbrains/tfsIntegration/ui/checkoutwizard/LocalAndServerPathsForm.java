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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.ui.servertree.TfsTreeForm;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

public class LocalAndServerPathsForm implements Disposable {

  private TextFieldWithBrowseButton myLocalPathField;
  private JPanel myContentPanel;
  private JLabel myMessageLabel;
  private TfsTreeForm myServerPathForm;
  private JLabel myServerPathLabel;
  private JLabel myLocalPathLabel;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public LocalAndServerPathsForm() {
    Disposer.register(this, myServerPathForm);

    myServerPathForm.addListener(new TfsTreeForm.SelectionListener() {
      @Override
      public void selectionChanged() {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });

    myLocalPathLabel.setLabelFor(myLocalPathField.getChildComponent());
    myLocalPathField.getChildComponent().getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });

    FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
    ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> listener =
      new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(TFSBundle.message("choose.local.path.title"),
                                                                           TFSBundle.message("choose.local.path.description"),
                                                                           myLocalPathField, null, descriptor,
                                                                           TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

    myServerPathLabel.setLabelFor(myServerPathForm.getPreferredFocusedComponent());
    myLocalPathField.addActionListener(listener);
    myMessageLabel.setIcon(UIUtil.getBalloonWarningIcon());
  }

  public void initialize(ServerInfo server, String initialPath) {
    myServerPathForm.initialize(server, initialPath, true, false, null);
  }

  public String getLocalPath() {
    return myLocalPathField.getText();
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public String getServerPath() {
    return myServerPathForm.getSelectedPath();
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void setMessage(String text, boolean error) {
    if (text != null) {
      myMessageLabel.setVisible(true);
      myMessageLabel.setText(text);
      myMessageLabel.setIcon(error ? UIUtil.getBalloonWarningIcon() : TfsTreeForm.EMPTY_ICON);
    }
    else {
      myMessageLabel.setVisible(false);
    }
  }

  public JComponent getPreferredFocusedComponent() {
    return myServerPathForm.getPreferredFocusedComponent();
  }

  @Override
  public void dispose() {
  }
}
