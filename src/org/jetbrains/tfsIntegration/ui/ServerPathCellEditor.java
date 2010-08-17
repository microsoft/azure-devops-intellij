/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.CellEditorComponentWithBrowseButton;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerPathCellEditor extends AbstractTableCellEditor {
  private final String myTitle;

  private CellEditorComponentWithBrowseButton<JTextField> myComponent;
  private final Project myProject;
  private final ServerInfo myServer;

  public ServerPathCellEditor(String title, Project project, ServerInfo server) {
    myTitle = title;
    myProject = project;
    myServer = server;
  }

  public Object getCellEditorValue() {
    return myComponent.getChildComponent().getText();
  }

  public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, int column) {
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ServerBrowserDialog d = new ServerBrowserDialog(myTitle, myProject, myServer, (String)getCellEditorValue(), true, false);
        d.show();
        if (d.isOK()) {
          myComponent.getChildComponent().setText(d.getSelectedPath());
        }
      }
    };
    myComponent = new CellEditorComponentWithBrowseButton<JTextField>(new TextFieldWithBrowseButton(listener), this);
    myComponent.getChildComponent().setText((String)value);
    return myComponent;
  }

}
