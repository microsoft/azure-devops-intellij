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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.labels.ItemAndVersion;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;

public class LabelItemsTableModel extends AbstractTableModel {

  enum Column {
    Item("Item", 300) {
      public String getValue(final ItemAndVersion itemAndVersion) {
        return itemAndVersion.getServerPath();
      }},
    Version("Version", 100) {
      public String getValue(final ItemAndVersion itemAndVersion) {
        return ((VersionSpecBase)itemAndVersion.getVersionSpec()).getPresentableString();
      }
    };

    private final String myName;
    private final int myWidth;

    private Column(final String name, final int width) {
      myName = name;
      myWidth = width;
    }

    public String getName() {
      return myName;
    }

    public int getWidth() {
      return myWidth;
    }

    public abstract String getValue(final ItemAndVersion itemAndVersion);
  }

  private List<ItemAndVersion> myContent;

  public LabelItemsTableModel() {
    myContent = Collections.emptyList();
  }

  public void setContent(final @NotNull List<ItemAndVersion> newContent) {
    myContent = newContent;
    fireTableDataChanged();
  }

  public int getRowCount() {
    return myContent.size();
  }

  public int getColumnCount() {
    return Column.values().length;
  }

  public String getColumnName(final int column) {
    return Column.values()[column].getName();
  }

  public Object getValueAt(final int rowIndex, final int columnIndex) {
    return Column.values()[columnIndex].getValue(getItem(rowIndex));
  }

  public ItemAndVersion getItem(final int rowIndex) {
    return myContent.get(rowIndex);
  }

}
