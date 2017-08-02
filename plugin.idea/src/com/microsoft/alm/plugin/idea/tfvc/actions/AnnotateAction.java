// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.actions.OpenFileInBrowserAction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Action to open up the annotate page in the browser for the selected file
 */
public class AnnotateAction extends SingleItemAction {
    private static final Logger logger = LoggerFactory.getLogger(OpenFileInBrowserAction.class);

    protected AnnotateAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_MSG));
    }

    @Override
    public void doUpdate(@NotNull final AnActionEvent e) {
        final VirtualFile file = VcsUtil.getOneVirtualFile(e);

        // disable for directories
        if (file == null || file.isDirectory()) {
            e.getPresentation().setEnabled(false);
            return;
        }
        // this will disable all new files
        super.doUpdate(e);
    }

    @Override
    protected void execute(final @NotNull SingleItemActionContext actionContext) {
        final ServerContext context = actionContext.getServerContext();

        // check for null values (it's ok if the server item is null because the URL redirects to the general page in that case
        if (context != null && context.getTeamProjectReference() != null && actionContext.getItem() != null) {
            final URI urlToBrowseTo = UrlHelper.getTfvcAnnotateURI(context.getUri().toString(),
                    context.getTeamProjectReference().getName(), actionContext.getItem().getServerItem());
            logger.info("Browsing to url " + urlToBrowseTo.getPath());
            BrowserUtil.browse(urlToBrowseTo);
        } else {
            final String issue = context == null ? "context is null" : context.getTeamProjectReference() == null ? "team project is null" : "getItem is null";
            logger.warn("Couldn't create annotate url: " + issue);
            Messages.showErrorDialog(actionContext.getProject(), TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_MSG),
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_TITLE));
        }
    }

    @Override
    protected String getProgressMessage() {
        return TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_STATUS);
    }
}