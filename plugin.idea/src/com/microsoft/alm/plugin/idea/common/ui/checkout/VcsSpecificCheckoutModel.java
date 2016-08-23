// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;

public interface VcsSpecificCheckoutModel {
    void doCheckout(final Project project, final CheckoutProvider.Listener listener,
                    final ServerContext context, final VirtualFile destinationParent,
                    final String directoryName, final String parentDirectory, final boolean isAdvancedChecked);

    String getTelemetryAction();

    String getButtonText();

    String getRepositoryName(final ServerContext context);

    RepositoryContext.Type getRepositoryType();
}
