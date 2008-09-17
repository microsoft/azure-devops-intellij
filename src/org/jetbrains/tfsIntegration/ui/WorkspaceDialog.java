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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserAction;
import org.jetbrains.tfsIntegration.ui.servertree.ServerBrowserDialog;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

// TODO: mnemonics i18n?

// TODO disable save button if name is empty

public class WorkspaceDialog extends DialogWrapper implements ActionListener {

  private enum Column {
    Status("Status") {
      public String getValue(WorkingFolderInfo workingFolderInfo) {
        return workingFolderInfo.getStatus().toString();
      }
    },
    LocalPath("Local path") {
      public String getValue(WorkingFolderInfo workingFolderInfo) {
        return workingFolderInfo.getLocalPath().getPresentableUrl();
      }
    },
    ServerPath("Server path") {
      public String getValue(WorkingFolderInfo workingFolderInfo) {
        return workingFolderInfo.getServerPath();
      }
    };

    private String myCaption;

    Column(String caption) {
      myCaption = caption;
    }

    public String getCaption() {
      return myCaption;
    }

    public abstract String getValue(WorkingFolderInfo workingFolderInfo);

  }

  private static class WorkingFoldersTableModel extends AbstractTableModel {
    private List<WorkingFolderInfo> myWorkingFolders;

    public void setWorkingFolders(List<WorkingFolderInfo> workingFolders) {
      myWorkingFolders = workingFolders;
      fireTableDataChanged();
    }

    public List<WorkingFolderInfo> getWorkingFolders() {
      return myWorkingFolders;
    }

    public String getColumnName(final int column) {
      return Column.values()[column].getCaption();
    }

    public int getRowCount() {
      return myWorkingFolders != null ? myWorkingFolders.size() : 0;
    }

    public int getColumnCount() {
      return Column.values().length;
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return Column.values()[columnIndex].getValue(myWorkingFolders.get(rowIndex));
    }

    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
      return true;
    }

    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
      WorkingFolderInfo workingFolder = myWorkingFolders.get(rowIndex);
      if (Column.Status.ordinal() == columnIndex) {
        WorkingFolderInfo.Status status = (WorkingFolderInfo.Status)aValue;
        workingFolder.setStatus(status);
      }
      else if (Column.ServerPath.ordinal() == columnIndex) {
        workingFolder.setServerPath((String)aValue);
      }
      else if (Column.LocalPath.ordinal() == columnIndex) {
        workingFolder.setLocalPath(VcsUtil.getFilePath((String)aValue));
      }
      else {
        throw new IllegalArgumentException("columnIndex: " + columnIndex);
      }
    }
  }

  private final Project myProject;
  private final WorkspaceInfo myWorkspace;

  private JTextField myNameField;
  private JButton myRemoveFolderButton;
  private WorkingFoldersTableModel myWorkingFoldersTableModel;
  private JTable myFoldersTable;
  private JTextArea myCommentArea;

  public WorkspaceDialog(Project project, WorkspaceInfo workspaceInfo) {
    super(project, true);
    myProject = project;
    myWorkspace = workspaceInfo;

    setResizable(true);

    init();
  }

  protected void init() {
    super.init();
    if (myWorkspace.getName() != null) {
      setTitle("Edit Workspace");
    }
    else {
      setTitle("Create Workspace");
    }
    setOKButtonText("Save");
  }

  protected String getDimensionServiceKey() {
    return "tfs.workspaceDialog";
  }

  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints gc = new GridBagConstraints();

    gc.gridx = 0;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets(3, 0, 0, 0);

    JLabel serverLabel = new JLabel("Server:");
    serverLabel.setDisplayedMnemonic('S');
    panel.add(serverLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 0;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(3, 0, 0, 0);

    JTextField serverTextField = new JTextField(myWorkspace.getServer().getUri().toString());
    serverTextField.setEditable(false);
    serverLabel.setLabelFor(serverTextField);
    panel.add(serverTextField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets(3, 0, 0, 0);

    JLabel nameLabel = new JLabel("Name:");
    nameLabel.setDisplayedMnemonic('N');
    panel.add(nameLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 1;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(3, 0, 0, 0);

    myNameField = new JTextField(myWorkspace.getName());
    nameLabel.setLabelFor(myNameField);
    panel.add(myNameField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 2;
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets(3, 0, 0, 0);

    JLabel ownerLabel = new JLabel("Owner:");
    ownerLabel.setDisplayedMnemonic('O');
    panel.add(ownerLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 2;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(3, 0, 0, 0);

    JTextField ownerTextField = new JTextField(myWorkspace.getOwnerName());
    ownerTextField.setEditable(false);
    ownerLabel.setLabelFor(ownerTextField);
    panel.add(ownerTextField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 3;
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets(3, 0, 0, 0);

    JLabel computerLabel = new JLabel("Computer:");
    computerLabel.setDisplayedMnemonic('C');
    panel.add(computerLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 3;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(3, 0, 0, 0);

    JTextField computerTextField = new JTextField(myWorkspace.getComputer());
    computerTextField.setEditable(false);
    computerLabel.setLabelFor(computerTextField);
    panel.add(computerTextField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 4;
    gc.anchor = GridBagConstraints.NORTHWEST;
    gc.insets = new Insets(3, 0, 100, 0);

    JLabel commentLabel = new JLabel("Comment:");
    commentLabel.setDisplayedMnemonic('m');
    panel.add(commentLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 4;
    gc.weighty = 0.5;
    gc.fill = GridBagConstraints.BOTH;
    gc.insets = new Insets(3, 0, 10, 0);

    myCommentArea = new JTextArea(myWorkspace.getComment());
    commentLabel.setLabelFor(myCommentArea);

    JScrollPane commentScrollPane = new JScrollPane(myCommentArea);
    commentScrollPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    panel.add(commentScrollPane, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 6;
    gc.gridwidth = 2;
    gc.weightx = 1.0;
    gc.weighty = 1.0;
    gc.fill = GridBagConstraints.BOTH;

    JPanel workingFoldersPanel = new JPanel(new GridBagLayout());
    workingFoldersPanel.setBorder(BorderFactory.createTitledBorder("Working folders:"));
    panel.add(workingFoldersPanel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1.0;
    gc.weighty = 1.0;
    gc.gridwidth = 3;
    gc.fill = GridBagConstraints.BOTH;

    myWorkingFoldersTableModel = new WorkingFoldersTableModel();
    myFoldersTable = new JTable(myWorkingFoldersTableModel);
    myFoldersTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    myFoldersTable.getColumn(Column.LocalPath.getCaption()).setCellEditor(new PathCellEditor(new PathCellEditor.ButtonDelegate() {

      public String processButtonClick(final String initialText) {
        // TODO refactor to use FilePath-s
        FileChooserDescriptor d = new FileChooserDescriptor(false, true, false, false, false, false);
        d.setTitle("Choose Local Folder");
        d.setShowFileSystemRoots(true);
        d.setDescription("Choose local folder to be mapped to server path");

        VirtualFile[] files = FileChooser.chooseFiles(panel, d, VcsUtil.getVirtualFile(initialText));
        if (files.length != 1 || files[0] == null) {
          return initialText;
        }
        return files[0].getPath().replace('/', File.separatorChar);
      }
    }));
    myFoldersTable.getColumn(Column.ServerPath.getCaption()).setCellEditor(new PathCellEditor(new PathCellEditor.ButtonDelegate() {

      public String processButtonClick(final String initialText) {
        ServerBrowserDialog d;
        try {
          getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          d = new ServerBrowserDialog("Choose Server Folder", myProject, myWorkspace.getServer(), initialText, true,
                                      Collections.<ServerBrowserAction>emptyList());
        }
        finally {
          getContentPane().setCursor(Cursor.getDefaultCursor());
        }
        d.show();
        if (d.isOK()) {
          final String selectedPath = d.getSelectedPath();
          if (selectedPath != null) {
            return selectedPath;
          }
        }
        return initialText;
      }
    }));

    myFoldersTable.getColumn(Column.Status.getCaption())
      .setCellEditor(new DefaultCellEditor(new JComboBox(new EnumComboBoxModel<WorkingFolderInfo.Status>(WorkingFolderInfo.Status.class))));

    JScrollPane tableScrollPane = new JScrollPane(myFoldersTable);
    tableScrollPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    myFoldersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      public void valueChanged(final ListSelectionEvent e) {
        updateButtons();
      }
    });
    workingFoldersPanel.add(tableScrollPane, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;

    JButton addFolderButton = new JButton("Add");
    workingFoldersPanel.add(addFolderButton, gc);
    addFolderButton.setMnemonic('a');

    addFolderButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent e) {
        myWorkspace.addWorkingFolderInfo(new WorkingFolderInfo());
        updateControls();
      }
    });

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 1;

    myRemoveFolderButton = new JButton("Remove");
    workingFoldersPanel.add(myRemoveFolderButton, gc);
    myRemoveFolderButton.setMnemonic('r');

    myRemoveFolderButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent e) {
        for (int row : myFoldersTable.getSelectedRows()) {
          WorkingFolderInfo folderInfo = myWorkingFoldersTableModel.getWorkingFolders().get(row);
          myWorkspace.removeWorkingFolderInfo(folderInfo);
        }
        updateControls();
      }
    });

    updateControls();
    return panel;
  }

  private void updateControls() {
    try {
      myWorkingFoldersTableModel.setWorkingFolders(myWorkspace.getWorkingFolders());
    }
    catch (TfsException e) {
      String message = MessageFormat.format("Failed to refresh workspace ''{0}''.\n{1}", myWorkspace.getName(), e.getLocalizedMessage());
      Messages.showErrorDialog(getContentPane(), message, getTitle());
    }
    myFoldersTable.getSelectionModel().clearSelection();
    updateButtons();
  }

  private void updateButtons() {
    myRemoveFolderButton.setEnabled(myFoldersTable.getSelectedRowCount() == 1);
  }

  protected void doOKAction() {
    myWorkspace.setName(myNameField.getText());
    myWorkspace.setComment(myCommentArea.getText());
    super.doOKAction();
  }

  public void actionPerformed(final ActionEvent e) {
  }

}