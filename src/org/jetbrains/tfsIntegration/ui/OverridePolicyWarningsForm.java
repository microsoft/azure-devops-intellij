/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.table.TableView;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.PolicyFailure;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.List;

public class OverridePolicyWarningsForm {

  public interface Listener extends EventListener {
    void stateChanged();
  }

  private JLabel myIconLabel;
  private TableView<PolicyFailure> myWarningsTable;
  private JCheckBox myOverrideCheckBox;
  private JTextArea myReasonTextArea;
  private JPanel myContentPane;

  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  public OverridePolicyWarningsForm(final Project project, List<PolicyFailure> failures) {
    myIconLabel.setIcon(Messages.getWarningIcon());

    myWarningsTable.setTableHeader(null);
    myWarningsTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          final PolicyFailure failure = myWarningsTable.getSelectedObject();
          if (failure != null) {
            failure.activate(project);
          }
        }
      }
    });

    myWarningsTable.setModel(new ListTableModel<PolicyFailure>(new ColumnInfo[]{CheckinParametersForm.WARNING_COLUMN_INFO}, failures, -1));

    myOverrideCheckBox.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        myReasonTextArea.setEnabled(myOverrideCheckBox.isSelected());
        if (myReasonTextArea.isEnabled()) {
          myReasonTextArea.requestFocus();
        }
        myEventDispatcher.getMulticaster().stateChanged();
      }
    });

    myReasonTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        myEventDispatcher.getMulticaster().stateChanged();
      }
    });
  }

  @Nullable
  public String getReason() {
    return myOverrideCheckBox.isSelected() ? myReasonTextArea.getText() : null;
  }

  public void addListener(Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

}
