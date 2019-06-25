// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.extensions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.util.NotNullFunction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.workitem.VcsWorkItemsController;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Extension to show the Work Items tab on the Version Control toolbar
 */
public class VcsWorkItemContentProvider implements ChangesViewContentProvider {
    private final static Logger logger = LoggerFactory.getLogger(VcsWorkItemContentProvider.class);

    private VcsWorkItemsController controller;
    private Throwable t;

    public VcsWorkItemContentProvider(@NotNull final Project project) {
        try {
            this.controller = new VcsWorkItemsController(project);
        } catch (Throwable t) {
            this.t = t;
            this.controller = null;
            logger.error("VcsWorkItemContentProvider: unexpected fatal error initializing the controller", t);
        }
    }

    @Override
    public JComponent initContent() {
        if (controller != null) {
            return controller.getPanel();
        } else {
            return new JLabel(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_UNEXPECTED_ERRORS, t != null ? t.getMessage() : StringUtils.EMPTY),
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
     * Check to see if the Work Item tab should be visible (TFVC/Git only and in Rider it must be a VSTS repo)
     */
    public static class VcsWorkItemVisibilityPredicate implements NotNullFunction<Project, Boolean> {
        @NotNull
        @Override
        public Boolean fun(final Project project) {
            // has to be TFVC or Git to be visible
            if (!VcsHelper.isGitVcs(project) && !VcsHelper.isTfVcs(project)) {
                return false;
            }

            return IdeaHelper.isRider() ? VcsHelper.isVstsRepo(project) : true;
        }
    }
}
