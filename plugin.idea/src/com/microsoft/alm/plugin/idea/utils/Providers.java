// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Providers {
    public static final Logger logger = LoggerFactory.getLogger(Providers.class);

    public static class ServerContextProvider {

        public ServerContext getAuthenticatedServerContext(@Nullable final Project project, @NotNull final GitRepository gitRepository) {

            if (ApplicationManager.getApplication() != null && ApplicationManager.getApplication().isDispatchThread()) {
                final String gitRemoteUrl = TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl();

                //Get auth info to force the intelliJ password store dialog to show up on the UI thread
                final AuthenticationInfo authInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(gitRemoteUrl, false);
                logger.debug("getAuthenticatedServerContext: Auth info = {} for url = {}", authInfo, gitRemoteUrl);

                final SettableFuture<ServerContext> future = SettableFuture.create();

                final Task.Backgroundable authenticationTask = new Task.Backgroundable(project,
                        TfPluginBundle.message(TfPluginBundle.KEY_AUTH_MSG_AUTHENTICATING),
                        false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        final ServerContext context = ServerContextManager.getInstance().getAuthenticatedContext(gitRemoteUrl, true);
                        future.set(context);
                    }
                };
                authenticationTask.queue();

                // Don't wait any longer than 15 minutes for the user to authenticate
                try {
                    return future.get(15, TimeUnit.MINUTES);
                } catch (Throwable t) {
                    logger.warn("getAuthenticatedServerContext: Authentication not complete after waiting for 15 minutes", t);
                }
                return null;
            } else {
                logger.warn("getAuthenticatedServerContext: This method has to be called on a UI thread");
                return null;
            }
        }
    }

    public static class GitRepositoryProvider {
        public GitRepository getGitRepository(@NotNull final Project project) {
            return TfGitHelper.getTfGitRepository(project);
        }
    }
}