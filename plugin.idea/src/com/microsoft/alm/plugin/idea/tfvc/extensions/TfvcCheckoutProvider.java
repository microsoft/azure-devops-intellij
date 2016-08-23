// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.checkout.CheckoutController;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.checkout.TfvcCheckoutModel;
import git4idea.actions.BasicAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TfvcCheckoutProvider implements CheckoutProvider {

    private final Logger logger = LoggerFactory.getLogger(TfvcCheckoutProvider.class);

    @Override
    public String getVcsName() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC);
    }

    @Override
    public void doCheckout(@NotNull final Project project, final Listener listener) {
        BasicAction.saveAll();

        if (!IdeaHelper.isTFConfigured(project)) {
            return;
        }

        // TF is configured, proceed with checkout
        try {
            final CheckoutController controller = new CheckoutController(project, listener, new TfvcCheckoutModel());
            controller.showModalDialog();
        } catch (Throwable t) {
            logger.warn("doCheckout failed unexpectedly", t);
            VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_UNEXPECTED, t.getMessage()));
        }
    }
}
