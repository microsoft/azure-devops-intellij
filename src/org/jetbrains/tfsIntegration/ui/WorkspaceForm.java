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
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.*;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceForm {

  private JTextField myNameField;
  private JLabel myServerField;
  private JLabel myOwnerField;
  private JLabel myComputerField;
  private JTextArea myCommentField;
  private JPanel myContentPane;
  private JPanel myTableWrapper;
  private ValidatingTableEditor<WorkingFolderInfo> myTable;
  private JLabel myMessageLabel;
  private JLabel myWorkingFoldrersLabel;
  private ComboBox myLocationField;
  private ServerInfo myServer;
  private final Project myProject;
  @Nullable private String myWorkingFolderValidationMessage;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  private static ColumnInfo<WorkingFolderInfo, Object> STATUS_COLUMN =
    new ColumnInfo<WorkingFolderInfo, Object>(TFSBundle.message("working.folder.status.column")) {
      @Override
      public Object valueOf(WorkingFolderInfo item) {
        return item.getStatus();
      }

      @Override
      public void setValue(WorkingFolderInfo item, Object value) {
        item.setStatus((WorkingFolderInfo.Status)value);
      }

      @Override
      public boolean isCellEditable(WorkingFolderInfo workingFolderInfo) {
        return true;
      }

      @Override
      public int getWidth(JTable table) {
        return 80;
      }

      @Override
      public TableCellEditor getEditor(WorkingFolderInfo o) {
        return new AbstractTableCellEditor() {
          private ComboBox myCombo;

          public Object getCellEditorValue() {
            return myCombo.getSelectedItem();
          }

          public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            ComboBoxModel model = new EnumComboBoxModel<WorkingFolderInfo.Status>(WorkingFolderInfo.Status.class);
            model.setSelectedItem(value);
            myCombo = new ComboBox(model, getWidth(table));
            return myCombo;
          }
        };
      }
    };

  private class LocalPathColumn extends ColumnInfo<WorkingFolderInfo, String> implements ValidatingTableEditor.RowHeightProvider {

    public LocalPathColumn() {
      super(TFSBundle.message("working.folder.local.path.column"));
    }

    @Override
    public String valueOf(WorkingFolderInfo item) {
      return item.getLocalPath().getPresentableUrl();
    }

    @Override
    public boolean isCellEditable(WorkingFolderInfo workingFolderInfo) {
      return true;
    }

    @Override
    public void setValue(WorkingFolderInfo item, String value) {
      item.setLocalPath(VcsUtil.getFilePath(value));
    }

    @Override
    public TableCellEditor getEditor(final WorkingFolderInfo item) {
      return new LocalPathCellEditor(TFSBundle.message("select.local.path.title"), myProject);
    }

    public int getRowHeight() {
      return new JTextField().getPreferredSize().height + 1;
    }

  }

  private ColumnInfo<WorkingFolderInfo, String> SERVER_PATH_COLUMN =
    new ColumnInfo<WorkingFolderInfo, String>(TFSBundle.message("working.folder.server.path.column")) {
      @Override
      public String valueOf(WorkingFolderInfo item) {
        return item.getServerPath();
      }

      @Override
      public void setValue(WorkingFolderInfo item, String value) {
        item.setServerPath(value);
      }

      @Override
      public boolean isCellEditable(WorkingFolderInfo item) {
        return true;
      }

      @Override
      public TableCellEditor getEditor(final WorkingFolderInfo item) {
        return new ServerPathCellEditor(TFSBundle.message("choose.server.path.dialog.title"), myProject, myServer);
      }
    };

  private void createUIComponents() {
    myTable = new ValidatingTableEditor<WorkingFolderInfo>() {
      @Override
      protected WorkingFolderInfo cloneOf(WorkingFolderInfo item) {
        return item.getCopy();
      }

      @Override
      protected WorkingFolderInfo createItem() {
        String path = myProject.isDefault() ? "" : myProject.getBaseDir().getPath();
        return new WorkingFolderInfo(VcsUtil.getFilePath(path));
      }

      @Nullable
      protected String validate(WorkingFolderInfo item) {
        if (StringUtil.isEmpty(item.getLocalPath().getPath())) {
          return TFSBundle.message("local.path.is.empty");
        }
        if (StringUtil.isEmpty(item.getServerPath())) {
          return TFSBundle.message("server.path.is.empty");
        }
        if (!item.getServerPath().startsWith(VersionControlPath.ROOT_FOLDER)) {
          return TFSBundle.message("server.path.is.invalid");
        }
        return null;
      }

      @Override
      protected void displayMessageAndFix(@Nullable Pair<String, Fix> messageAndFix) {
        myWorkingFolderValidationMessage = messageAndFix != null ? messageAndFix.first : null;
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    };
    myTable.hideMessageLabel();
    myTable.setColumnReorderingAllowed(false);

    myTableWrapper = new JPanel(new BorderLayout());
    myTableWrapper.add(myTable.getContentPane());
  }

  private void setupLocations() {
    for (WorkspaceInfo.Location location : WorkspaceInfo.Location.values()) {
      myLocationField.addItem(location);
    }
  }

  private WorkspaceForm(final Project project) {
    myProject = project;

    myWorkingFoldrersLabel.setLabelFor(myTable.getPreferredFocusedComponent());
    DocumentAdapter listener = new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(e));
      }
    };
    myNameField.getDocument().addDocumentListener(listener);
    myCommentField.getDocument().addDocumentListener(listener);

    myMessageLabel.setIcon(UIUtil.getBalloonWarningIcon());

    setupLocations();
  }

  public WorkspaceForm(Project project, @NotNull ServerInfo server) {
    this(project);
    myServer = server;
    myServerField.setText(myServer.getPresentableUri());
    myOwnerField.setText(myServer.getQualifiedUsername());
    myComputerField.setText(Workstation.getComputerName());

    myTable.setModel(new ColumnInfo[]{STATUS_COLUMN, new LocalPathColumn(), SERVER_PATH_COLUMN}, new ArrayList<WorkingFolderInfo>());
  }

  public WorkspaceForm(Project project, @NotNull WorkspaceInfo workspace) {
    this(project, workspace.getServer());
    myNameField.setText(workspace.getName());
    myLocationField.setSelectedItem(workspace.getLocation());
    myCommentField.setText(workspace.getComment());
    myTable.setModel(new ColumnInfo[]{STATUS_COLUMN, new LocalPathColumn(), SERVER_PATH_COLUMN},
                     new ArrayList<WorkingFolderInfo>(workspace.getWorkingFoldersCached()));
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public String getWorkspaceName() {
    return myNameField.getText();
  }

  @NotNull
  public WorkspaceInfo.Location getWorkspaceLocation() {
    return (WorkspaceInfo.Location)myLocationField.getSelectedItem();
  }

  public String getWorkspaceComment() {
    return myCommentField.getText();
  }

  public List<WorkingFolderInfo> getWorkingFolders() {
    return myTable.getItems();
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void setErrorMessage(@Nullable final String message) {
    myMessageLabel.setText(message);
    myMessageLabel.setVisible(message != null);
  }

  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Nullable
  public String validateWorkingFolders() {
    return myWorkingFolderValidationMessage;
  }


}
