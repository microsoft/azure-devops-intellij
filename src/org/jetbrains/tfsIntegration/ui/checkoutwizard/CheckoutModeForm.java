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

import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CheckoutModeForm {

  private JPanel myContentPanel;
  private JRadioButton myAutoModeButton;
  private JRadioButton myManualModeButton;
  private JTextField myWorkspaceNameField;
  private JLabel myErrorLabel;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public CheckoutModeForm() {
    myErrorLabel.setIcon(UIUtil.getBalloonWarningIcon());

    myAutoModeButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
        IdeFocusManager.findInstanceByComponent(myContentPanel).requestFocus(myWorkspaceNameField, true);
      }
    });
    myManualModeButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });
    myWorkspaceNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myAutoModeButton.setSelected(true);
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  public boolean isAutoModeSelected() {
    return myAutoModeButton.isSelected();
  }

  public void setAutoModeSelected(boolean selected) {
    if (selected) {
      myAutoModeButton.setSelected(true);
    }
    else {
      myManualModeButton.setSelected(true);
    }
  }

  public String getNewWorkspaceName() {
    return myWorkspaceNameField.getText();
  }

  public void setNewWorkspaceName(String name) {
    myWorkspaceNameField.setText(name);
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void setErrorMessage(String message) {
    myErrorLabel.setText(message);
    myErrorLabel.setVisible(message != null);
  }

  public JComponent getPreferredFocusedComponent() {
    if (myAutoModeButton.isSelected()) {
      return myWorkspaceNameField;
    } else {
      return myManualModeButton;
    }
  }

}
