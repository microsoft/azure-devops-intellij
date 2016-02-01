// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.vcsUtil.AuthDialog;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.CredentialsPrompt;

/**
 * Credentials prompt implementation for the IntelliJ plugin.
 * We use the built in AuthDialog and grab the default project.
 */
public class CredentialsPromptImpl implements CredentialsPrompt {
    private String userName;
    private String password;
    private boolean promptSuccess;
    private RuntimeException validationError;
    private String authenticationUrl;

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
        // Test the authenticatedContext against the server
        validationError = null;
        authenticationUrl = null;
        if (UrlHelper.isGitRemoteUrl(serverUrl)) {
            tryToParseDeploymentUrl(serverUrl, authenticationInfo);
            if (validationError != null) {
                throw validationError;
            }
        } else {
            // Assume it is a deployment url
            tryDeploymentUrl(serverUrl, authenticationInfo, true);
        }

        // validation succeeded, return the authenticated url that worked
        return authenticationUrl;
    }

    private void tryToParseDeploymentUrl(final String gitRemoteUrl, final AuthenticationInfo authenticationInfo) {
        UrlHelper.ParseResult result = UrlHelper.tryParse(gitRemoteUrl, new UrlHelper.ParseResultValidator() {
            @Override
            public boolean validate(final UrlHelper.ParseResult parseResult) {
                return tryDeploymentUrl(parseResult.getServerUrl(), authenticationInfo, false);
            }
        });
    }

    private boolean tryDeploymentUrl(final String deploymentUrl, final AuthenticationInfo authenticationInfo, final boolean throwOnFailure) {
        validationError = null;
        final ServerContext context =
                new ServerContextBuilder().type(ServerContext.Type.TFS)
                        .uri(deploymentUrl).authentication(authenticationInfo).build();
        try {
            ServerContextManager.getInstance().validateServerConnection(context);
            // Save the authenticated url for later use
            authenticationUrl = deploymentUrl;
            return true;
        } catch (RuntimeException ex) {
            validationError = ex;
            if (throwOnFailure) {
                throw ex;
            }

            return false;
        }
    }


}
