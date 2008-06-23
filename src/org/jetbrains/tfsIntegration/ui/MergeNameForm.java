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

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MergeNameForm {
  private JRadioButton myKeepLocalRadioButton;
  private JRadioButton myTakeServerRadioButton;
  private JRadioButton myUseAnotherRadioButton;
  private JTextField myAnotherNameTextField;
  private JPanel myContentPanel;
  private String myLocalName;
  private String myServerName;

  public MergeNameForm(final String localName, final String serverName) {
    myLocalName = localName;
    myServerName = serverName;

    myKeepLocalRadioButton.setText(myKeepLocalRadioButton.getText() + " (" + myLocalName + ")");
    myKeepLocalRadioButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          myAnotherNameTextField.setEnabled(false);
        }
    });

    myTakeServerRadioButton.setText(myTakeServerRadioButton.getText() + " (" + myServerName + ")");
    myTakeServerRadioButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          myAnotherNameTextField.setEnabled(false);
        }
    });
    myAnotherNameTextField.setText(myLocalName);
    myUseAnotherRadioButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          myAnotherNameTextField.setEnabled(true);
        }
    });
  }

  public JComponent getPanel() {
    return myContentPanel;
  }

  public String getSelectedName() {
    if (myKeepLocalRadioButton.isSelected()) {
      return myLocalName;
    }
    else if (myTakeServerRadioButton.isSelected()) {
      return myServerName;
    }
    else if (myUseAnotherRadioButton.isSelected()) {
      return myAnotherNameTextField.getText();
    }
    return null;
  }
}
