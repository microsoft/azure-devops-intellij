// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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
package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.CellEditorComponentWithBrowseButton;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.tfvc.ui.servertree.ServerBrowserDialog;

import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerPathCellEditor extends AbstractTableCellEditor {
    private final String title;
    private final Project project;
    private final ServerContext serverContext;

    private CellEditorComponentWithBrowseButton<JTextField> component;

    public ServerPathCellEditor(final String title, final Project project, final ServerContext serverContext) {
        this.title = title;
        this.project = project;
        this.serverContext = serverContext;
    }

    public Object getCellEditorValue() {
        return component.getChildComponent().getText();
    }

    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ServerBrowserDialog dialog = new ServerBrowserDialog(title, project, serverContext, (String) getCellEditorValue(), true, false);
                if (dialog.showAndGet()) {
                    component.getChildComponent().setText(dialog.getSelectedPath());
                }
            }
        };
        component = new CellEditorComponentWithBrowseButton<JTextField>(new TextFieldWithBrowseButton(listener), this);
        component.getChildComponent().setText((String) value);
        return component;
    }
}
