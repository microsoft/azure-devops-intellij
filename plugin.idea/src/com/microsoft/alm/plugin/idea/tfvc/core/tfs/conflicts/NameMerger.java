// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.external.models.RenameConflict;
import org.jetbrains.annotations.Nullable;

public interface NameMerger {

    /**
     * @param conflict
     * @param project
     * @return null if user cancelled
     */
    @Nullable
    String mergeName(final RenameConflict conflict, Project project);
}
