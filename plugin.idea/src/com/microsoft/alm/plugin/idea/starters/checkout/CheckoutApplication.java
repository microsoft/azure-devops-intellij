// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.starters.checkout;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.checkout.CompositeCheckoutListener;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.simplecheckout.SimpleCheckoutController;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checkouts a VSTS project using a Git Url given from the commandline
 */
public class CheckoutApplication extends ApplicationStarterBase {
    private final Logger logger = LoggerFactory.getLogger(CheckoutApplication.class);

    @Override
    public String getUsageMessage() {
        // the IDE that is being used (i.e. idea, charm, etc.)
        final String applicationType = ApplicationNamesInfo.getInstance().getScriptName();
        return String.format("Usage:\n\t%s " + CHECKOUT_COMMAND + " <git url>", applicationType);
    }

    @Override
    protected void processCommand(final String[] args, @Nullable String currentDirectory) throws Exception {
        // already verified that args[1] exists and is the url
        final String gitUrl = args[1];
        final Project project = DefaultProjectFactory.getInstance().getDefaultProject();
        final CheckoutProvider.Listener listener = new CompositeCheckoutListener(project);

        try {
            launchApplicationWindow();

            final SimpleCheckoutController controller = new SimpleCheckoutController(project, listener, gitUrl);
            controller.showModalDialog();
        } catch (Throwable t) {
            //unexpected error occurred while doing simple checkout
            logger.warn("VSTS commandline checkout failed due to an unexpected error", t);
            VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_UNEXPECTED, t.getMessage()));
        }
    }

    /**
     * Launch IntelliJ window without any project or start menu showing
     */
    private void launchApplicationWindow() {
        SystemDock.updateMenu();
        WindowManagerImpl windowManager = (WindowManagerImpl) WindowManager.getInstance();
        IdeEventQueue.getInstance().setWindowManager(windowManager);
        windowManager.showFrame();
    }
}
