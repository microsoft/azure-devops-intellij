// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.actions.StandardVcsGroup;

public class TFSGroup extends StandardVcsGroup {

    public AbstractVcs getVcs(Project project) {
        return TFSVcs.getInstance(project);
    }

    @Override
    public String getVcsName(final Project project) {
        return TFSVcs.TFVC_NAME;
    }
}
