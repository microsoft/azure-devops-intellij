// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
