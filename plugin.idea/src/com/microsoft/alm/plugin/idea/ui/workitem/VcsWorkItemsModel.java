// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.Providers;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItem;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class VcsWorkItemsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(VcsWorkItemsModel.class);

    private final Project project;

    private final WorkItemsTableModel tableModel;
    private final WorkItemsLookupListener treeDataProvider;
    private GitRepository gitRepository;
    private ServerContext context;

    private boolean connected = false;
    private boolean authenticated = false;
    private boolean authenticating = false;
    private boolean loading = false;
    private boolean loadingErrors = false;

    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_AUTHENTICATED = "authenticated";
    public static final String PROP_AUTHENTICATING = "authenticating";
    public static final String PROP_LOADING = "loading";
    public static final String PROP_LOADING_ERRORS = "loadingErrors";
    public final static String PROP_SERVER_NAME = "serverName";


    public VcsWorkItemsModel(final @NotNull Project project) {
        this.project = project;

        tableModel = new WorkItemsTableModel(WorkItemsTableModel.DEFAULT_COLUMNS);
        treeDataProvider = new WorkItemsLookupListener(this, tableModel);
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

    public WorkItemsTableModel getTableModel() {
        return tableModel;
    }

    private boolean connectionSetup() {
        if (connected && gitRepository != null && authenticated && context != null) {
            logger.debug("connectionSetup: connection is good");
            return true;
        }

        gitRepository = new Providers.GitRepositoryProvider().getGitRepository(project);
        if (gitRepository == null) {
            setConnected(false);
            logger.debug("connectionSetup: Failed to get Git repo for current project");
            return false;
        }
        setConnected(true);

        setAuthenticating(true);
        context = new Providers.ServerContextProvider().getAuthenticatedServerContext(project, gitRepository);
        setAuthenticating(false);

        if (context == null) {
            setAuthenticated(false);
            logger.debug("connectionSetup: failed to get authenticated context for current repo");
            return false;
        }
        setAuthenticated(true);

        //connection setup successfully
        return true;
    }

    public void loadWorkItems() {
        if (!connectionSetup()) {
            return;
        }
        setLoading(true);
        tableModel.clearRows();
        treeDataProvider.loadWorkItems(context);
    }

    public void importIntoTeamServicesGit() {
        final ImportController controller = new ImportController(project);
        controller.showModalDialog();
    }

    public void openSelectedWorkItemsLink() {
        final List<WorkItem> workItems = tableModel.getSelectedWorkItems();

        if (context != null && context.getTeamProjectURI() != null) {
            final URI teamProjectURI = context.getTeamProjectURI();
            if (teamProjectURI != null) {
                for (WorkItem item : workItems) {
                    super.gotoLink(UrlHelper.getSpecificWorkItemURI(teamProjectURI, item.getId()).toString());
                }
            } else {
                logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
            }
        }
    }

    public void clearWorkItems() {
        tableModel.clearRows();
    }

    public void createNewWorkItemLink() {
        if (!connectionSetup()) {
            return;
        }
        if (context != null && context.getTeamProjectURI() != null) {
            final URI teamProjectURI = context.getTeamProjectURI();
            if (teamProjectURI != null) {
                super.gotoLink(UrlHelper.getCreateWorkItemURI(teamProjectURI).toString());
            } else {
                logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
            }
        }
    }

    public void dispose() {
        treeDataProvider.terminateActiveOperation();
    }
}