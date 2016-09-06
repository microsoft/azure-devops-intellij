// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ContentTriplet;

import java.io.IOException;

public interface ContentMerger {

    boolean mergeContent(final String conflict, final ContentTriplet contentTriplet, final Project project, final VirtualFile targetFile,
                         final String localPath, final VcsRevisionNumber serverVersion)
            throws IOException, VcsException;

}
