// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionListener;

public class ResolveConflictsDialog extends BaseDialogImpl {
    private ResolveConflictsForm resolveConflictsForm;

    public ResolveConflictsDialog(final Project project) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_DIALOG_LATER),
                TfPluginBundle.KEY_TFVC_CONFLICT_DIALOG_TITLE);
    }

    @Nullable
    protected JComponent createCenterPanel() {
        resolveConflictsForm = new ResolveConflictsForm();
        return resolveConflictsForm.getContentPanel();
    }

    public void setConflictsTableModel(final ConflictsTableModel conflictsTableModel) {
        // adds listener to close the dialog when the conflict table is empty
        conflictsTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {
                if (conflictsTableModel.getRowCount() == 0) {
                    ResolveConflictsDialog.this.close(OK_EXIT_CODE);
                }
            }
        });

        resolveConflictsForm.setModelForView(conflictsTableModel);
    }

    public int[] getSelectedRows() {
        return resolveConflictsForm.getSelectedRows();
    }

    public void setLoading(final boolean isLoading) {
        resolveConflictsForm.setLoading(isLoading);
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
        resolveConflictsForm.addActionListener(listener);
    }

    /**
     * Override so only OK action is created and not Cancel
     *
     * @return
     */
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}
