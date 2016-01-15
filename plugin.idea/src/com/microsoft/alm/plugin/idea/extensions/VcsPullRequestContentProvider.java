// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.microsoft.alm.plugin.idea.ui.pullrequest.VcsPullRequestsController;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * Extension to show the Pull Requests tab on the Version Control toolbar
 */
public class VcsPullRequestContentProvider implements ChangesViewContentProvider {
    private final VcsPullRequestsController controller;

    public VcsPullRequestContentProvider(@NotNull final Project project) {
        this.controller = new VcsPullRequestsController(project);
    }

    @Override
    public JComponent initContent() {
        return controller.getPanel();
    }

    @Override
    public void disposeContent() {
        controller.dispose();
    }
}
