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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.util.EventDispatcher;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserAction;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserDialog;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

public class WorkspaceForm {

  public interface Listener extends EventListener {
    void dataChanged();
  }

  private JTextField myNameField;
  private JLabel myServerField;
  private JLabel myOwnerField;
  private JLabel myComputerField;
  private JTextArea myCommentField;
  private JTable myFoldersTable;
  private JPanel myContentPane;
  private JButton myAddButton;
  private JButton myRemoveButton;
  private JLabel myErrorLabel;
  private final WorkingFoldersTableModel myWorkingFoldersTableModel;
  private final Project myProject;
  private ServerInfo myServer;

  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  public WorkspaceForm(final Project project) {
    myProject = project;

    myAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        //noinspection ConstantConditions
        FilePath projectRootPath =
          myProject.getBaseDir() != null ? TfsFileUtil.getFilePath(myProject.getBaseDir()) : VcsUtil.getFilePath("");
        myWorkingFoldersTableModel.addWorkingFolder(new WorkingFolderInfo(projectRootPath));
        updateControls();
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        myWorkingFoldersTableModel.removeWorkingFolders(myFoldersTable.getSelectedRows());
        updateControls();
      }
    });

    myWorkingFoldersTableModel = new WorkingFoldersTableModel();
    //noinspection HardCodedStringLiteral
    myFoldersTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    myFoldersTable.setModel(myWorkingFoldersTableModel);
    myFoldersTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    for (int i = 0; i < WorkingFoldersTableModel.Column.values().length; i++) {
      myFoldersTable.getColumnModel().getColumn(i).setPreferredWidth(WorkingFoldersTableModel.Column.values()[i].getWidth());
    }

    myFoldersTable.setDefaultRenderer(WorkingFolderInfo.Status.class, new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final boolean hasFocus,
                                                     final int row,
                                                     final int column) {
        String text = ((WorkingFolderInfo.Status)value).name();
        return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
      }
    });

    final JComboBox statusCombo = new JComboBox(new EnumComboBoxModel<WorkingFolderInfo.Status>(WorkingFolderInfo.Status.class));
    statusCombo.setBorder(BorderFactory.createEmptyBorder());
    myFoldersTable.setDefaultEditor(WorkingFolderInfo.Status.class, new DefaultCellEditor(statusCombo));

    myFoldersTable.setDefaultRenderer(FilePath.class, new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final boolean hasFocus,
                                                     final int row,
                                                     final int column) {
        String text = ((FilePath)value).getPresentableUrl();
        return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
      }
    });

    myFoldersTable
      .setDefaultEditor(FilePath.class, new FieldWithButtonCellEditor<FilePath>(false, new FieldWithButtonCellEditor.Helper<FilePath>() {
        public String toStringRepresentation(@Nullable final FilePath value) {
          return value != null ? value.getPresentableUrl() : "";
        }

        public FilePath fromStringRepresentation(@Nullable final String stringRepresentation) {
          return StringUtil.isEmptyOrSpaces(stringRepresentation) ? null : VcsUtil.getFilePath(stringRepresentation);
        }

        public String processButtonClick(final String initialText) {
          FileChooserDescriptor d = new FileChooserDescriptor(false, true, false, false, false, false);
          d.setTitle("Choose Local Path");
          d.setShowFileSystemRoots(true);
          d.setDescription("Choose local folder to be mapped to server path");

          VirtualFile[] files = FileChooser.chooseFiles(getContentPane(), d, VcsUtil.getVirtualFile(initialText));
          if (files.length != 1 || files[0] == null) {
            return initialText;
          }
          return files[0].getPath().replace('/', File.separatorChar);
        }
      }));

    myFoldersTable
      .setDefaultEditor(String.class, new FieldWithButtonCellEditor<String>(false, new FieldWithButtonCellEditor.Helper<String>() {
        public String toStringRepresentation(@Nullable final String value) {
          return value != null ? value : "";
        }

        public String fromStringRepresentation(@Nullable final String stringRepresentation) {
          return StringUtil.isEmptyOrSpaces(stringRepresentation) ? null : stringRepresentation;
        }

        public String processButtonClick(final String initialText) {
          ServerBrowserDialog d;
          try {
            getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            d = new ServerBrowserDialog("Choose Server Path", myProject, myServer, initialText, true,
                                        Collections.<ServerBrowserAction>emptyList());
          }
          finally {
            getContentPane().setCursor(Cursor.getDefaultCursor());
          }
          d.show();
          if (d.isOK()) {
            final ServerTree.SelectedItem selectedPath = d.getSelectedPath();
            if (selectedPath != null) {
              return selectedPath.path;
            }
          }
          return initialText;
        }
      }));

    myFoldersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        updateControls();
      }
    });

    myNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myEventDispatcher.getMulticaster().dataChanged();
      }
    });

    myCommentField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myEventDispatcher.getMulticaster().dataChanged();
      }
    });

    myWorkingFoldersTableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent e) {
        myEventDispatcher.getMulticaster().dataChanged();
      }
    });

    updateControls();
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public void init(final @NotNull ServerInfo server) {
    myServer = server;
    myServerField.setText(myServer.getPresentableUri());
    myOwnerField.setText(myServer.getQualifiedUsername());
    myComputerField.setText(Workstation.getComputerName());
    myWorkingFoldersTableModel.setWorkingFolders(new ArrayList<WorkingFolderInfo>());
  }

  public void init(final @NotNull WorkspaceInfo workspace) throws TfsException {
    init(workspace.getServer());
    myNameField.setText(workspace.getName());
    myCommentField.setText(workspace.getComment());
    myWorkingFoldersTableModel.setWorkingFolders(new ArrayList<WorkingFolderInfo>(workspace.getWorkingFoldersCached()));
  }

  private void updateControls() {
    myRemoveButton.setEnabled(myFoldersTable.getSelectedRowCount() > 0);
  }


  public String getWorkspaceName() {
    return myNameField.getText();
  }

  public String getWorkspaceComment() {
    return myCommentField.getText();
  }

  public List<WorkingFolderInfo> getWorkingFolders() {
    return myWorkingFoldersTableModel.getWorkingFolders();
  }

  public void addListener(Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(Listener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void setErrorMessage(@Nullable final String message) {
    myErrorLabel.setText(message != null ? message : " ");
  }

  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

}
