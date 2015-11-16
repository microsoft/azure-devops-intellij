// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.checkout.CheckoutController;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import git4idea.actions.BasicAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitCheckoutProvider implements CheckoutProvider {

    private final Logger logger = LoggerFactory.getLogger(GitCheckoutProvider.class);

    @Override
    public String getVcsName() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TF_GIT);
    }

    @Override
    public void doCheckout(@NotNull final Project project, final Listener listener) {
        BasicAction.saveAll();

        if(!IdeaHelper.isGitExeConfigured(project)) {
            return;
        }

        //Git.exe is configured, proceed with checkout
        try {

            final CheckoutController controller = new CheckoutController(project, listener);
            controller.showModalDialog();
        } catch (Throwable t) {
            //unexpected error
            logger.warn("doCheckout failed unexpected error", t);
            VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_UNEXPECTED, t.getMessage()));
        }
    }
}
