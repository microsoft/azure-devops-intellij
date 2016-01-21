// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
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

    private boolean connected = false;
    private boolean authenticated = false;
    private boolean authenticating = false;
    private boolean loading = false;
    private boolean loadingErrors = false;
    private Date lastRefreshed;

    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_AUTHENTICATED = "authenticated";
    public static final String PROP_AUTHENTICATING = "authenticating";
    public static final String PROP_LOADING = "loading";
    public static final String PROP_LOADING_ERRORS = "loadingErrors";
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

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(final boolean authenticated) {
        if (this.authenticated != authenticated) {
            this.authenticated = authenticated;
            setChangedAndNotify(PROP_AUTHENTICATED);
        }
    }

    public boolean isAuthenticating() {
        return authenticating;
    }

    public void setAuthenticating(final boolean authenticating) {
        if (this.authenticating != authenticating) {
            this.authenticating = authenticating;
            setChangedAndNotify(PROP_AUTHENTICATING);
        }
    }

    public boolean hasLoadingErrors() {
        return loadingErrors;
    }

    public void setLoadingErrors(final boolean loadingErrors) {
        if (this.loadingErrors != loadingErrors) {
            this.loadingErrors = loadingErrors;
            setChangedAndNotify(PROP_LOADING_ERRORS);
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
        if (connected && gitRepository != null && authenticated && context != null) {
            return true; //connection is good
        }

        gitRepository = gitRepositoryProvider.getGitRepository(project);
        if (gitRepository == null) {
            setConnected(false);
            return false; //failed to get Git repo for current project
        }
        setConnected(true);

        setAuthenticating(true);
        context = serverContextProvider.getAuthenticatedServerContext(gitRepository);
        setAuthenticating(false);

        if (context == null) {
            setAuthenticated(false);
            return false; //failed to get authenticated context for current repo
        }
        setAuthenticated(true);

        //connection setup successfully
        return true;
    }

    public void loadPullRequests() {
        if (!connectionSetup()) {
            return;
        }

        clearPullRequests();
        treeDataProvider.loadPullRequests(context);
    }

    public void importIntoTeamServicesGit() {
        final ImportController controller = new ImportController(project);
        controller.showModalDialog();

        //TODO: how do we know this is done to load pull requests?
    }

    public void openGitRepoLink() {
        if (context != null && context.getGitRepository() != null) {
            if (StringUtils.isNotEmpty(context.getGitRepository().getRemoteUrl())) {
                BrowserUtil.browse(context.getGitRepository().getRemoteUrl().concat("/pullrequests"));
            }
        }
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

        //TODO: how do we know this is done to refresh the tree?
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
            //TODO: seems to hang
            final String gitRemoteUrl = TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl();
            return ServerContextManager.getInstance().getAuthenticatedContext(gitRemoteUrl, true);
        }
    }

    void setServerContextProvider(final ServerContextProvider serverContextProvider) {
        this.serverContextProvider = serverContextProvider;
    }
}
