// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class MockFileSelector extends IdeaFileSelector {
    @Nullable
    private Collection<VirtualFile> forcedFiles;

    @Nullable
    @Override
    public Collection<VirtualFile> selectFiles(
            Project project,
            List<VirtualFile> originalFiles,
            @Nls String prompt,
            VcsShowConfirmationOption confirmationOption,
            @Nls String dialogTitle) {
        if (forcedFiles != null)
            return forcedFiles;

        return super.selectFiles(project, originalFiles, prompt, confirmationOption, dialogTitle);
    }

    public void forceReturnSelectedFiles(Collection<VirtualFile> files) {
        forcedFiles = files;
    }
}
