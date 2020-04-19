// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.DialogManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class IdeaFileSelector {
    public static IdeaFileSelector getInstance() {
        return ServiceManager.getService(IdeaFileSelector.class);
    }

    @Nullable
    public Collection<VirtualFile> selectFiles(
            Project project,
            List<VirtualFile> originalFiles,
            @Nls String prompt,
            VcsShowConfirmationOption confirmationOption,
            @Nls String dialogTitle) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        SelectFilesDialog dialog = SelectFilesDialog.init(
                project,
                originalFiles,
                prompt,
                confirmationOption,
                true,
                false,
                false);
        dialog.setTitle(dialogTitle);
        DialogManager.show(dialog);
        return dialog.isOK() ? dialog.getSelectedFiles() : null;
    }
}
