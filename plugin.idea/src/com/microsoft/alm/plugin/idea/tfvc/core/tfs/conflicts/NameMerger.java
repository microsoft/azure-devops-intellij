// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public interface NameMerger {

    /**
     * @param nameChoice1
     * @param nameChoice2
     * @param project
     * @return null if user cancelled
     */
    @Nullable
    String mergeName(final String nameChoice1, final String nameChoice2, final Project project);
}
