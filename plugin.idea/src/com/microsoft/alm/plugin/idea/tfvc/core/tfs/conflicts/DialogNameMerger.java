// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.MergeNameDialog;
import org.jetbrains.annotations.Nullable;

public class DialogNameMerger implements NameMerger {

    @Nullable
    public String mergeName(final String nameChoice1, final String nameChoice2, final Project project) {
        final MergeNameDialog dialog = new MergeNameDialog(nameChoice1, nameChoice2, project);
        if (dialog.showAndGet()) {
            return dialog.getSelectedPath();
        }
        return null;
    }
}