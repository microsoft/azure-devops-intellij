// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;

public interface ConflictsHandler {
    void resolveConflicts(final Project project, final ResolveConflictHelper conflictHelper) throws TfsException;
}
