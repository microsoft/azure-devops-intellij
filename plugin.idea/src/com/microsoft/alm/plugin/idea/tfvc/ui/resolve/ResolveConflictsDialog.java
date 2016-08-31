// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class ResolveConflictsDialog extends BaseDialogImpl {
    private ResolveConflictsForm resolveConflictsForm;

    public ResolveConflictsDialog(final Project project) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_DIALOG_FINISHED),
                TfPluginBundle.KEY_TFVC_CONFLICT_DIALOG_TITLE);
    }

//TODO: handle skipped conflicts
//  protected void doOKAction() {
//    for (String conflict : myResolveConflictHelper.getConflicts()) {
//      myResolveConflictHelper.skip(conflict);
//    }
//    super.doOKAction();
//  }

    @Nullable
    protected JComponent createCenterPanel() {
        resolveConflictsForm = new ResolveConflictsForm();
        return resolveConflictsForm.getContentPanel();
    }

    public void setConflictsTableModel(final ConflictsTableModel conflictsTableModel) {
        resolveConflictsForm.setModelForView(conflictsTableModel);
    }

    public int[] getSelectedConflicts() {
        return resolveConflictsForm.getSelectedConflicts();
    }

    public void setLoading(final boolean isLoading) {
        resolveConflictsForm.setLoading(isLoading);
    }
}
