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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DoubleClickListener;
import com.intellij.util.EventDispatcher;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.VersionControlLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;


public class SelectLabelForm {

  public interface Listener extends EventListener {
    void selectionChanged();
  }

  private JTextField myNameField;
  private JTextField myOwnerField;
  private JButton myFindButton;
  private JTable myLabelsTable;
  private JPanel myContentPane;

  private final LabelsTableModel myLabelsTableModel;

  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  public SelectLabelForm(final SelectLabelDialog dialog, final WorkspaceInfo workspace) {
    myLabelsTableModel = new LabelsTableModel();
    myLabelsTable.setModel(myLabelsTableModel);
    myLabelsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    myLabelsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        myEventDispatcher.getMulticaster().selectionChanged();
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        if (isLabelSelected()) {
          dialog.close(DialogWrapper.OK_EXIT_CODE);
          return true;
        }
        return false;
      }
    }.installOn(myLabelsTable);

    myFindButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          String owner = myOwnerField.getText().trim();
          if ("".equals(owner)) {
            owner = null;
          }
          String name = myNameField.getText().trim();
          if ("".equals(name)) {
            name = null;
          }

          List<VersionControlLabel> labels =
            workspace.getServer().getVCS()
              .queryLabels(name, VersionControlPath.ROOT_FOLDER, owner, false, null, null, false, getContentPane(),
                           TFSBundle.message("searching.for.label"));
          myLabelsTableModel.setLabels(labels);
        }
        catch (TfsException ex) {
          myLabelsTableModel.setLabels(Collections.<VersionControlLabel>emptyList());
          Messages.showErrorDialog(myContentPane, ex.getMessage(), "Find Label");
        }
        finally {
          myEventDispatcher.getMulticaster().selectionChanged();
        }
      }
    });
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public void addListener(final Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(final Listener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public boolean isLabelSelected() {
    return myLabelsTable.getSelectedRowCount() == 1;
  }

  @NotNull
  public VersionControlLabel getLabel() {
    return myLabelsTableModel.getLabels().get(myLabelsTable.getSelectedRow());
  }

}
