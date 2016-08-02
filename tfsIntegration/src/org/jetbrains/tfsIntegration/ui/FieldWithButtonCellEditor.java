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

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.util.EventObject;

public class FieldWithButtonCellEditor<T> extends DefaultCellEditor {

  public interface Helper<T> {
    String toStringRepresentation(@Nullable T value);

    @Nullable
    T fromStringRepresentation(@Nullable String stringRepresentation);

    String processButtonClick(String initialText);
  }

  public FieldWithButtonCellEditor(boolean pathCompletion, final Helper<T> helper) {
    super(new JTextField());
    setClickCountToStart(1);

    final TextFieldWithBrowseButton field =
      pathCompletion ? new TextFieldWithBrowseButton() : new TextFieldWithBrowseButton.NoPathCompletion();
    field.setOpaque(false);
    field.getTextField().setBorder(BorderFactory.createEmptyBorder());

    field.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        String result = helper.processButtonClick(field.getText());
        if (result != null) {
          field.setText(result);
        }
      }
    });

    delegate = new EditorDelegate() {
      public void setValue(Object value) {
        //noinspection unchecked
        field.setText(helper.toStringRepresentation((T)value));
      }

      @Nullable
      public Object getCellEditorValue() {
        return helper.fromStringRepresentation(field.getText());
      }

      public boolean shouldSelectCell(EventObject anEvent) {
        if (anEvent instanceof MouseEvent) {
          MouseEvent e = (MouseEvent)anEvent;
          return e.getID() != MouseEvent.MOUSE_DRAGGED;
        }
        return true;
      }
    };
    editorComponent = field;
  }

}
