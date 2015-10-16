// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import org.jetbrains.annotations.NotNull;

//TODO remove this unused class
public class Notifications {
    public static final Logger LOG = Logger.getInstance("TfIntellijPlugin");

    public static void showError(@NotNull Project project,
                                 @NotNull String title,
                                 @NotNull String message,
                                 @NotNull String logDetails) {
        LOG.warn(title + "; " + message + "; " + logDetails);
        VcsNotifier.getInstance(project).notifyError(title, message);
    }
}
