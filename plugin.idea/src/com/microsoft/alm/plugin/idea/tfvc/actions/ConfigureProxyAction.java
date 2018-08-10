// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.ProxySettingsDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action allows the user to add/remove/edit the proxy url for the current TFVC server.
 * Note that we are currently just using the property service to save and restore the proxy settings.
 * The key for the property is PROXY_repository_server_url
 */
public class ConfigureProxyAction extends DumbAwareAction {
    private static final Logger logger = LoggerFactory.getLogger(ConfigureProxyAction.class);

    @VisibleForTesting
    public ConfigureProxyAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_PROXY_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_PROXY_MSG),
                null);
    }

    @Override
    public void actionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        final RepositoryContext context = VcsHelper.getRepositoryContext(project);

        if (context != null) {
            final String currentProxy = getProxy(context);
            final ProxySettingsDialog dialog = new ProxySettingsDialog(project, context.getUrl(), currentProxy);
            if (dialog.showAndGet()) {
                final String newProxy = dialog.getProxyUri();
                setProxy(context, newProxy);
            }
        } else {
            logger.warn("RepositoryContext returned null");
        }
    }

    private String getProxy(final RepositoryContext context) {
        if (context == null) {
            return null;
        }
        return WorkspaceHelper.getProxyServer(context.getUrl());
    }

    private void setProxy(final RepositoryContext context, final String newProxy) {
        if (context == null) {
            return;
        }
        WorkspaceHelper.setProxyServer(context.getUrl(), newProxy);
    }

    @Override
    public void update(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        anActionEvent.getPresentation().setEnabled(project != null);
    }
}
