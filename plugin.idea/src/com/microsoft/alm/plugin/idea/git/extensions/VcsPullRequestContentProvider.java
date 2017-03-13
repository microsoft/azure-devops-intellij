// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.extensions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.util.NotNullFunction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.git.ui.pullrequest.VcsPullRequestsController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Extension to show the Pull Requests tab on the Version Control toolbar
 */
public class VcsPullRequestContentProvider implements ChangesViewContentProvider {
    private final static Logger logger = LoggerFactory.getLogger(VcsPullRequestContentProvider.class);

    private VcsPullRequestsController controller;
    private Throwable t;

    public VcsPullRequestContentProvider(@NotNull final Project project) {
        if (VcsHelper.isGitVcs(project)) {
            try {
                this.controller = new VcsPullRequestsController(project);
            } catch (Throwable t) {
                this.t = t;
                this.controller = null;
                logger.error("VcsPullRequestContentProvider: unexpected fatal error initializing the controller", t);
            }
        } else {
            this.controller = null;
        }
    }

    @Override
    public JComponent initContent() {
        if (controller != null) {
            return controller.getPanel();
        } else {
            return new JLabel(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_UNEXPECTED_ERRORS, t.getMessage()),
                    AllIcons.General.Warning, 0);
        }
    }

    @Override
    public void disposeContent() {
        if (controller != null) {
            controller.dispose();
        }
    }

    /**
     * Check to see if the Pull Request tab should be visible (Rider only)
     */
    public static class VcsPullRequestVisibilityPredicate implements NotNullFunction<Project, Boolean> {
        @NotNull
        @Override
        public Boolean fun(final Project project) {
            // has to be a Git repo to be visible
            if (!VcsHelper.isGitVcs(project)) {
                return false;
            }
            return IdeaHelper.isRider() ? VcsHelper.isVstsRepo(project) : true;
        }
    }
}