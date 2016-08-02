// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.vcsUtil.AuthDialog;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.CredentialsPrompt;

/**
 * Credentials prompt implementation for the IntelliJ plugin.
 * We use the built in AuthDialog and grab the default project.
 */
public class CredentialsPromptImpl implements CredentialsPrompt {
    private String userName;
    private String password;
    private boolean promptSuccess;

    @Override
    public boolean prompt(final String serverUrl, final String defaultUserName) {
        promptSuccess = false;
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                promptSuccess = promptInternal(serverUrl, defaultUserName);
            }
        }, true);

        return promptSuccess;
    }

    private boolean promptInternal(final String serverUrl, final String defaultUserName) {
        final AuthDialog authDialog = new AuthDialog(ProjectManager.getInstance().getDefaultProject(),
                TfPluginBundle.message(TfPluginBundle.KEY_PROMPT_CREDENTIALS_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_PROMPT_CREDENTIALS_MESSAGE, serverUrl),
                defaultUserName, null, true);

        if (authDialog.showAndGet()) {
            userName = authDialog.getUsername();
            password = authDialog.getPassword();
            return true;
        }

        return false;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String validateCredentials(final String serverUrl, final AuthenticationInfo authenticationInfo) {
        final ServerContext context =
                new ServerContextBuilder().type(ServerContext.Type.TFS)
                        .uri(serverUrl).authentication(authenticationInfo).build();
        ServerContextManager.getInstance().validateServerConnection(context);

        // validation succeeded, return the authenticated url that worked
        return serverUrl;
    }
}
