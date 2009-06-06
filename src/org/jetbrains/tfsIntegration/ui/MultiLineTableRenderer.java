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

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class MultiLineTableRenderer extends JTextArea implements TableCellRenderer {

  public MultiLineTableRenderer() {
    setLineWrap(true);
    setWrapStyleWord(true);
    setBorder(UIManager.getBorder("Table.cellNoFocusBorder"));
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (isSelected) {
      super.setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    }
    else {
      Color background = table.getBackground();
      if (background == null || background instanceof javax.swing.plaf.UIResource) {
        Color alternateColor = UIManager.getColor("Table.alternateRowColor");
        if (alternateColor != null && row % 2 == 0) background = alternateColor;
      }
      super.setForeground(table.getForeground());
      super.setBackground(background);
    }

    setFont(table.getFont());

    if (hasFocus) {
      if (!isSelected && table.isCellEditable(row, column)) {
        Color col;
        col = UIManager.getColor("Table.focusCellForeground");
        if (col != null) {
          super.setForeground(col);
        }
        col = UIManager.getColor("Table.focusCellBackground");
        if (col != null) {
          super.setBackground(col);
        }
      }
    }

    customize(table, this, value);

    setSize(table.getColumnModel().getColumn(column).getWidth(), 100000);
    table.setRowHeight(row, getPreferredSize().height);
    return this;
  }

  protected abstract void customize(JTable table, JTextArea textArea, Object value);

}
