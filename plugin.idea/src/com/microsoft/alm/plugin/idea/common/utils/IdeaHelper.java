// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import git4idea.GitVcs;
import git4idea.config.GitExecutableValidator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class IdeaHelper {
    private static final Logger logger = LoggerFactory.getLogger(IdeaHelper.class);

    private static final String CHARSET_UTF8 = "utf-8";
    public static final String TEST_RESOURCES_SUB_PATH = "/externals/platform/";
    private static final String PROD_RESOURCES_SUB_PATH = "platform";

    public IdeaHelper() {
    }

    public static void setProgress(final ProgressIndicator indicator, final double fraction, final String text) {
        setProgress(indicator, fraction, text, false);
    }

    public static void setProgress(final ProgressIndicator indicator, final double fraction, final String text, final boolean delay) {
        if (indicator == null) {
            // Tests send null to skip showing progress
            return;
        }

        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                indicator.setFraction(fraction);
                indicator.setText(text);
            }
        });

        if (delay) {
            // Give time for the progress to be updated
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("setting progress failed", e);
            }
        }
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

    /**
     * Verifies if TF is configured, show notification and warning message if not
     *
     * @param project Idea project
     * @return true if TF is configured, false if TF is not correctly configured
     */
    public static boolean isTFConfigured(@NotNull final Project project) {
        final String tfLocation = TfTool.getLocation();
        if (StringUtils.isEmpty(tfLocation)) {
            //TF is not configured, show warning message
            Messages.showWarningDialog(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_NOT_CONFIGURED),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC));
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
            showErrorDialog(project, throwable.getMessage());
        } else {
            showErrorDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_MESSAGE_TEAM_SERVICES_UNEXPECTED_ERROR));
        }
    }

    /**
     * Shows an error dialog
     *
     * @param project
     * @param message
     */
    public static void showErrorDialog(@NotNull final Project project, @NotNull final String message) {
        if (ApplicationManager.getApplication() == null) {
            // No application manager means no IntelliJ, we are probably in a test context
            return;
        }

        Messages.showErrorDialog(project, message,
                TfPluginBundle.message(TfPluginBundle.KEY_TITLE_TEAM_SERVICES_ERROR));
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

    /**
     * Find the project that is associated with the current open frame
     * Got this method from how Git gets the current project
     *
     * @return project based on repo
     */
    public static Project getCurrentProject() {
        final IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        return frame == null || frame.getProject() == null ? ProjectManager.getInstance().getDefaultProject() : frame.getProject();
    }

}
