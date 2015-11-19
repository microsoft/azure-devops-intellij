// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import git4idea.GitVcs;
import git4idea.config.GitExecutableValidator;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class IdeaHelper {
    public IdeaHelper() {
    }

    /**
     * Verifies if Git exe is configured, show notification and warning message if not
     *
     * @param project Idea project
     * @return true if Git exe is configured, false if Git exe is not correctly configured
     */
    public static boolean isGitExeConfigured(@NotNull final Project project) {
        final GitExecutableValidator validator = GitVcs.getInstance(project).getExecutableValidator();
        if (!validator.checkExecutableAndNotifyIfNeeded()) {
            //Git.exe is not configured, show warning message in addition to notification from Git plugin
            Messages.showWarningDialog(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_GIT_NOT_CONFIGURED),
                    TfPluginBundle.message(TfPluginBundle.KEY_TF_GIT));
            return false;
        }

        return true;
    }

    public static void runOnUIThread(final Runnable runnable) {
        runOnUIThread(runnable, false);
    }

    public static void runOnUIThread(final Runnable runnable, final boolean wait) {
        runOnUIThread(runnable, wait,
                ApplicationManager.getApplication() != null ?
                        ApplicationManager.getApplication().getAnyModalityState() :
                        null);
    }

    public static void runOnUIThread(final Runnable runnable, final boolean wait, final ModalityState modalityState) {
        if (ApplicationManager.getApplication() != null && !ApplicationManager.getApplication().isDispatchThread()) {
            if (wait) {
                ApplicationManager.getApplication().invokeAndWait(runnable, modalityState);
            } else {
                ApplicationManager.getApplication().invokeLater(runnable, modalityState);
            }
        } else {
            // If we are already on the dispatch thread then we can run it here
            // If we don't have an application then we are testing, just run the runnable here
            runnable.run();
        }
    }

    /**
     * Shows a dialog with OK and cancel actions to prompt for confirmation from user
     * @return true if user clicks on ok action and false if user clicks on cancel action
     */
    public static boolean showConfirmationDialog(@NotNull final Project project, final String message, final String title,
                                                 final Icon logo, final String okActionMessage, final String cancelActionMessage) {

        final int result = Messages.showYesNoDialog(project, message, title, okActionMessage, cancelActionMessage, logo);
        return result == 0 ? true : false;
    }
}
