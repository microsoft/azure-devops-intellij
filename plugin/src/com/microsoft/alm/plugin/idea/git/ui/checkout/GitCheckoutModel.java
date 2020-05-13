// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.checkout;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.checkout.VcsSpecificCheckoutModel;
import git4idea.commands.Git;
import org.apache.commons.lang.StringUtils;

public class GitCheckoutModel implements VcsSpecificCheckoutModel {
    @Override
    public void doCheckout(
            Project project,
            CheckoutProvider.Listener listener,
            ServerContext context,
            VirtualFile destinationParent,
            String directoryName,
            String parentDirectory,
            boolean isAdvancedChecked,
            boolean isTfvcServerCheckout) {
        final String gitRepositoryStr = context.getUsableGitUrl();
        final Git git = ServiceManager.getService(Git.class);
        git4idea.checkout.GitCheckoutProvider.clone(project, git, listener,
                destinationParent,
                gitRepositoryStr,
                directoryName,
                parentDirectory);
    }

    @Override
    public String getButtonText() {
        return TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_CLONE_BUTTON);
    }

    @Override
    public String getRepositoryName(final ServerContext context) {
        return (context != null && context.getGitRepository() != null)
                ? context.getGitRepository().getName() : StringUtils.EMPTY;
    }

    @Override
    public RepositoryContext.Type getRepositoryType() {
        return RepositoryContext.Type.GIT;
    }
}
