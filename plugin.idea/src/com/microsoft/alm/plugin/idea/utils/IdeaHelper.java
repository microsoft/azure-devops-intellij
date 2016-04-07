// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import git4idea.GitVcs;
import git4idea.config.GitExecutableValidator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class IdeaHelper {
    private static final String CHARSET_UTF8 = "utf-8";
    public static final String TEST_RESOURCES_SUB_PATH = "/externals/platform/";
    private static final String PROD_RESOURCES_SUB_PATH = "platform";

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
     *
     * @return true if user clicks on ok action and false if user clicks on cancel action
     */
    public static boolean showConfirmationDialog(@NotNull final Project project, final String message, final String title,
                                                 final Icon logo, final String okActionMessage, final String cancelActionMessage) {

        final int result = Messages.showYesNoDialog(project, message, title, okActionMessage, cancelActionMessage, logo);
        return result == 0 ? true : false;
    }

    /**
     * Shows an error dialog
     *
     * @param project
     * @param throwable
     */
    public static void showErrorDialog(@NotNull final Project project, final Throwable throwable) {
        if (throwable != null) {
            Messages.showErrorDialog(project,
                    throwable.getMessage(),
                    TfPluginBundle.message(TfPluginBundle.KEY_TITLE_TEAM_SERVICES_ERROR));
        } else {
            Messages.showErrorDialog(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_MESSAGE_TEAM_SERVICES_UNEXPECTED_ERROR),
                    TfPluginBundle.message(TfPluginBundle.KEY_TITLE_TEAM_SERVICES_ERROR));
        }
    }

    public static boolean notifyOnAuthorizationError(@NotNull final String url, @NotNull final Project project, @NotNull final Throwable throwable) {
        //check for NotAuthorized 401 errors
        if (AuthHelper.isNotAuthorizedError(throwable)) {
            VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_TITLE_TEAM_SERVICES_ERROR),
                    TfPluginBundle.message(TfPluginBundle.KEY_AUTH_FAILED_REFRESH_NOTIFICATION), new NotificationListener() {
                        @Override
                        public void hyperlinkUpdate(@NotNull final Notification n, @NotNull final HyperlinkEvent e) {
                            ServerContextManager.getInstance().updateAuthenticationInfo(url);
                            n.expire();
                        }
                    });
            return true;
        }
        return false;
    }

    /**
     * Finds the full path to a resource whether it's installed by the idea or being run inside the idea
     *
     * @param resourceUrl  the URL for the resource
     * @param resourceName name of the resource
     * @param directory    the directory under the idea.plugin/resource directory to for the resource
     * @return the path to the resource
     * @throws UnsupportedEncodingException
     */
    public static String getResourcePath(final URL resourceUrl, final String resourceName, final String directory) throws UnsupportedEncodingException {
        // find location of the resource
        String resourcePath = resourceUrl.getPath();
        resourcePath = URLDecoder.decode(resourcePath, CHARSET_UTF8);
        resourcePath = StringUtils.chomp(resourcePath, "/");

        // When running the plugin inside of the idea for test, the path to the app needs to be
        // manipulated to look in a different location than where the resource resides in production.
        // For prod the url is .../../.IdeaIC15/config/plugins/com.microsoft.alm/lib but for test
        // the url is ../.IdeaIC15/system/plugins-sandbox/plugins/com.microsoft.alm.plugin.idea/classes
        if (resourcePath != null && resourcePath.endsWith(PROD_RESOURCES_SUB_PATH)) {
            return resourcePath + "/" + directory + "/" + resourceName;
        } else {
            return resourcePath + TEST_RESOURCES_SUB_PATH + directory + "/" + resourceName;
        }
    }

    /**
     * Check if a file is executable and if not sets it to be executable. When the plugin is unzipped,
     * the permissions of a file is not persisted so that is why a check is needed.
     *
     * @param executable executable file for application
     * @throws FileNotFoundException
     */
    public static void setExecutablePermissions(final File executable) throws FileNotFoundException {
        if (!executable.exists()) {
            throw new FileNotFoundException(executable.getPath() + " not found while trying to set permissions.");
        }

        // set the executable to execute for all users
        if (!executable.canExecute()) {
            executable.setExecutable(true, false);
        }
    }
}
