// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.extensions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.git.ui.pullrequest.VcsPullRequestsController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Extension to show the Pull Requests tab on the Version Control toolbar
 */
public class VcsPullRequestContentProvider implements ChangesViewContentProvider {
    private final static Logger logger = LoggerFactory.getLogger(VcsPullRequestContentProvider.class);

    private VcsPullRequestsController controller;
    private Throwable t;
    private boolean hideTab = false;

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
            this.hideTab = true;
        }
    }

    @Override
    public JComponent initContent() {
        if (controller != null) {
            return controller.getPanel();
        } else if (hideTab) {
            // TODO: find a way to hide tab instead of displaying message
            final JPanel panel = new JPanel(new BorderLayout());
            final JLabel errorMsg = new JLabel(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TFVC_MSG));
            errorMsg.setHorizontalAlignment(JLabel.CENTER);
            panel.add(errorMsg, BorderLayout.CENTER);
            return panel;
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
}
