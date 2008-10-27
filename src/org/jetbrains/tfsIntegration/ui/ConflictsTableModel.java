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

import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ConflictType;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ConflictsTableModel extends AbstractTableModel {

  public enum Column {

    Name("Name") {
      public String getValue(Conflict conflict) {
        if (conflict.getCtype() == ConflictType.Merge) {
          return conflict.getTgtlitem() != null ? conflict.getTgtlitem() : conflict.getSrclitem();
        }
        else {
          return conflict.getSrclitem() != null ? conflict.getSrclitem() : conflict.getTgtlitem();
        }
      }
    };

    private String myCaption;

    Column(String caption) {
      myCaption = caption;
    }

    public String getCaption() {
      return myCaption;
    }

    public abstract String getValue(Conflict conflict);

  }

  private List<Conflict> myConflicts;

  public List<Conflict> getMergeData() {
    return myConflicts;
  }

  public String getColumnName(final int column) {
    return Column.values()[column].getCaption();
  }

  public int getRowCount() {
    return myConflicts != null ? myConflicts.size() : 0;
  }

  public int getColumnCount() {
    return Column.values().length;
  }

  public Object getValueAt(final int rowIndex, final int columnIndex) {
    Conflict conflict = myConflicts.get(rowIndex);
    return Column.values()[columnIndex].getValue(conflict);
  }

  public List<Conflict> getConflicts() {
    return myConflicts;
  }

  public void setConflicts(final List<Conflict> conflicts) {
    myConflicts = conflicts;
    fireTableDataChanged();
  }

}
