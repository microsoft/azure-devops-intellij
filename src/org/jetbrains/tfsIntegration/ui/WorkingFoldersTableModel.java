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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class WorkingFoldersTableModel extends AbstractTableModel {

  public enum Column {
    Status("Status", WorkingFolderInfo.Status.class, 80) {
      public WorkingFolderInfo.Status getValue(WorkingFolderInfo workingFolder) {
        return workingFolder.getStatus();
      }
      public void setValue(final WorkingFolderInfo workingFolder, @Nullable final Object value) {
        //noinspection ConstantConditions
        workingFolder.setStatus((WorkingFolderInfo.Status)value);
      }
    },
    LocalPath("Local path", FilePath.class, 350) {
      public FilePath getValue(WorkingFolderInfo workingFolder) {
        return workingFolder.getLocalPath();
      }
      public void setValue(final WorkingFolderInfo workingFolder, @Nullable final Object value) {
        FilePath localPath = (FilePath)value;
        workingFolder.setLocalPath(localPath != null ? localPath : VcsUtil.getFilePath(""));
      }
    },
    ServerPath("Server path", String.class, 300) {
      public String getValue(WorkingFolderInfo workingFolder) {
        return workingFolder.getServerPath();
      }
      public void setValue(final WorkingFolderInfo workingFolder, @Nullable final Object value) {
        String serverPath = (String)value;
        workingFolder.setServerPath(serverPath != null ? serverPath : "");
      }
    };

    private final Class<?> myClass;
    private final String myCaption;
    private final int myWidth;

    Column(String caption, Class<?> clazz, int width) {
      myCaption = caption;
      myClass = clazz;
      myWidth = width;
    }

    public String getCaption() {
      return myCaption;
    }

    public Class<?> getColumnClass() {
      return myClass;
    }

    public int getWidth() {
      return myWidth;
    }

    public abstract Object getValue(WorkingFolderInfo workingFolderInfo);

    public abstract void setValue(WorkingFolderInfo workingFolderInfo, @Nullable Object value);
  }

  private List<WorkingFolderInfo> myWorkingFolders;

  public void setWorkingFolders(List<WorkingFolderInfo> workingFolders) {
    myWorkingFolders = new ArrayList<WorkingFolderInfo>(workingFolders);
    fireTableDataChanged();
  }

  public void addWorkingFolder(WorkingFolderInfo workingFolder) {
    myWorkingFolders.add(workingFolder);
    fireTableDataChanged();
  }

  public List<WorkingFolderInfo> getWorkingFolders() {
    return myWorkingFolders;
  }

  public void removeWorkingFolders(final int[] positions) {
    Collection<WorkingFolderInfo> toRemove = new ArrayList<WorkingFolderInfo>(positions.length);
    for (int position : positions) {
      toRemove.add(myWorkingFolders.get(position));
    }
    myWorkingFolders.removeAll(toRemove);
    fireTableDataChanged();
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

  public Class<?> getColumnClass(final int columnIndex) {
    return Column.values()[columnIndex].getColumnClass();
  }

  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return true;
  }

  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    WorkingFolderInfo workingFolder = myWorkingFolders.get(rowIndex);
    Column.values()[columnIndex].setValue(workingFolder, aValue);
  }
}
