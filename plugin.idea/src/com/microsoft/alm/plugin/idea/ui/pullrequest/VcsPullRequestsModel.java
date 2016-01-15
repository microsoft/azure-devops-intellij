// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class VcsPullRequestsModel extends AbstractModel {
    private final Project project;

    private final PullRequestsTreeModel treeModel;
    private final PullRequestsLookupListener treeDataProvider;

    private GitRepositoryProvider gitRepositoryProvider;
    private GitRepository gitRepository;

    private ServerContextProvider serverContextProvider;
    private ServerContext context;

    private boolean loading = false;
    private boolean connected = false;
    private Date lastRefreshed;

    public static final String PROP_LOADING = "loading";
    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_LAST_REFRESHED = "lastRefreshed";


    public VcsPullRequestsModel(@NotNull Project project) {
        this.project = project;

        gitRepositoryProvider = new GitRepositoryProvider();
        serverContextProvider = new ServerContextProvider();

        treeModel = new PullRequestsTreeModel();
        treeDataProvider = new PullRequestsLookupListener(this);
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(final boolean loading) {
        if (this.loading != loading) {
            this.loading = loading;
            setChangedAndNotify(PROP_LOADING);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(final boolean connected) {
        if (this.connected != connected) {
            this.connected = connected;
            setChangedAndNotify(PROP_CONNECTED);
        }
    }

    public void setLastRefreshed(final Date lastRefreshed) {
        if (this.lastRefreshed != lastRefreshed) {
            this.lastRefreshed = lastRefreshed;
            setChangedAndNotify(PROP_LAST_REFRESHED);
        }
    }

    public Date getLastRefreshed() {
        return lastRefreshed;
    }

    public PullRequestsTreeModel getPullRequestsTreeModel() {
        return treeModel;
    }

    private boolean connectionSetup() {
        if (connected && gitRepository != null && context != null) {
            return true; //connection is good
        }

        gitRepository = gitRepositoryProvider.getGitRepository(project);
        if (gitRepository == null) {
            return false; //failed to get Git repo for current project
        }

        context = serverContextProvider.getAuthenticatedServerContext(gitRepository);
        if (context == null) {
            return false; //failed to get authenticated context for current repo
        }

        //connection setup successfully
        setConnected(true);
        return true;
    }

    public void loadPullRequests() {
        if (!connectionSetup()) {
            return;
        }

        clearPullRequests();
        setLoading(true);

        final PullRequestLookupOperation operation = new PullRequestLookupOperation(context);
        operation.addListener(treeDataProvider);
        treeDataProvider.loadPullRequests(context);
    }

    public void appendPullRequests(final List<GitPullRequest> pullRequests, final PullRequestLookupOperation.PullRequestScope scope) {
        treeModel.appendPullRequests(pullRequests, scope);
    }

    public void clearPullRequests() {
        treeModel.clearPullRequests();
    }

    public void createNewPullRequest() {
        if (!connectionSetup()) {
            return;
        }

        final CreatePullRequestController createPullRequestController = new CreatePullRequestController(project, gitRepository);
        createPullRequestController.showModalDialog();
    }

    public void dispose() {
        treeDataProvider.terminateActiveOperation();
    }

    static class GitRepositoryProvider {
        public GitRepository getGitRepository(@NotNull final Project project) {
            return TfGitHelper.getTfGitRepository(project);
        }
    }

    void setGitRepositoryProvider(final GitRepositoryProvider gitRepositoryProvider) {
        this.gitRepositoryProvider = gitRepositoryProvider;
    }

    static class ServerContextProvider {
        public ServerContext getAuthenticatedServerContext(@NotNull final GitRepository gitRepository) {
            final String gitRemoteUrl = TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl();
            return ServerContextManager.getInstance().getAuthenticatedContext(gitRemoteUrl, true);
        }
    }

    void setServerContextProvider(final ServerContextProvider serverContextProvider) {
        this.serverContextProvider = serverContextProvider;
    }
}
