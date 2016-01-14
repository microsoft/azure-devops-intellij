// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.starters;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.checkout.CompositeCheckoutListener;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.simplecheckout.SimpleCheckoutController;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * This class starts the SimpleCheckout workflow given a URI or a Git Url
 */
public class SimpleCheckoutStarter implements StarterBase {
    private final Logger logger = LoggerFactory.getLogger(SimpleCheckoutStarter.class);
    public static final String SUB_COMMAND_NAME = "checkout";
    private static final String URL_ARGUMENT = "url";
    private static final String ENCODING_ARGUMENT = "EncFormat";
    private final String gitUrl;

    /**
     * Private constructor so that Git Url can be checked as a precaution
     *
     * @param gitUrl
     */
    private SimpleCheckoutStarter(final String gitUrl) {
        this.gitUrl = gitUrl;
    }

    /**
     * Creates a SimpleCheckoutStarter object after verifying Git Url
     *
     * @param gitUrl
     * @return SimpleCheckoutStarter with associated Git Url
     * @throws RuntimeException
     */
    public static SimpleCheckoutStarter createWithGitUrl(final String gitUrl) throws RuntimeException {
        if (!UrlHelper.isGitRemoteUrl(gitUrl)) {
            throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.STARTER_ERRORS_INVALID_GIT_URL, gitUrl));
        }

        return new SimpleCheckoutStarter(gitUrl);
    }

    /**
     * Creates a SimpleCheckoutStarter object after processing the commandline args given for checkout command
     *
     * @param args command line args
     * @return SimpleCheckoutStarter with associated Git Url passed in args
     * @throws RuntimeException
     */
    public static SimpleCheckoutStarter createWithCommandLineArgs(final List<String> args) throws RuntimeException {
        if (args.size() != 1) {
            throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.STARTER_ERRORS_SIMPLECHECKOUT_INVALID_COMMAND_LINE_ARGS));
        }

        return createWithGitUrl(args.get(0));
    }

    /**
     * Creates a SimpleCheckoutStarter object after parsing the URI attributes for the Git Url and Url encoding if provided
     *
     * @param args Uri attributes
     * @return SimpleCheckoutStarter with URI's decoded Git Url
     * @throws RuntimeException
     * @throws UnsupportedEncodingException
     */
    public static SimpleCheckoutStarter createWithUriAttributes(Map<String, String> args) throws RuntimeException, UnsupportedEncodingException {
        String url = args.get(URL_ARGUMENT);
        String encoding = args.get(ENCODING_ARGUMENT);

        if (StringUtils.isEmpty(url)) {
            throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.STARTER_ERRORS_SIMPLECHECKOUT_URI_MISSING_GIT_URL));
        }

        // if an encoding scheme is found then decode the url
        if (StringUtils.isNotEmpty(encoding)) {
            url = URLDecoder.decode(url, encoding);
        }

        return createWithGitUrl(url);
    }

    /**
     * Kicks off the SimpleCheckout workflow with the Git Url passed
     */
    public void processCommand() {
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

    /**
     * Getter used for testing currently
     *
     * @return gitUrl
     */
    protected String getGitUrl() {
        return gitUrl;
    }
}
