// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.ProjectManager;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItem;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ListSelectionModel;
import java.net.URI;

public class SelectWorkItemsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(SelectWorkItemsModel.class);

    public final static String PROP_LOADING = "loading";
    public final static String PROP_FILTER = "filter";
    public final static String PROP_SERVER_NAME = "serverName";

    private final WorkItemsTableModel tableModel;
    private final GitRepository gitRepository;
    private boolean loading = false;
    private String filter;
    private ServerContext latestServerContext;

    private boolean maxItemsReached = false;

    public SelectWorkItemsModel(final GitRepository gitRepository) {
        this.gitRepository = gitRepository;
        tableModel = new WorkItemsTableModel(WorkItemsTableModel.DEFAULT_COLUMNS);
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

    public boolean isMaxItemsReached() {
        return maxItemsReached;
    }

    //TODO replace server label on form with UserAccountControl
    public String getServerName() {
        if (latestServerContext != null) {
            return latestServerContext.getServerUri().toString();
        }
        return StringUtils.EMPTY;
    }

    public void loadWorkItems() {
        setLoading(true);
        tableModel.clearRows();

        final String gitRemoteUrl = TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl();

        WorkItemLookupOperation operation = new WorkItemLookupOperation(gitRemoteUrl);
        operation.addListener(new Operation.Listener() {
            @Override
            public void notifyLookupStarted() {
                // nothing to do
                logger.info("WorkItemLookupOperation started.");
            }

            @Override
            public void notifyLookupCompleted() {
                logger.info("WorkItemLookupOperation completed.");

                // Set loading to false to stop the spinner
                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                    }
                });
            }

            @Override
            public void notifyLookupResults(final Operation.Results results) {
                final WorkItemLookupOperation.WitResults wiResults = (WorkItemLookupOperation.WitResults) results;
                maxItemsReached = wiResults.maxItemsReached();

                if (wiResults.isCancelled()) {
                    // Do nothing
                } else {
                    // Update table model on UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (wiResults.hasError()) {
                                IdeaHelper.showErrorDialog(ProjectManager.getInstance().getDefaultProject(), wiResults.getError());
                            }

                            if (wiResults.getContext() != null) {
                                // Set the latestServerContext
                                latestServerContext = wiResults.getContext();
                                // Notify observers that the server name changed
                                setChangedAndNotify(PROP_SERVER_NAME);
                            }

                            tableModel.addWorkItems(wiResults.getWorkItems());
                        }
                    });
                }
            }
        });

        operation.doWorkAsync(new WorkItemLookupOperation.WitInputs(
                WorkItemHelper.getAssignedToMeQuery(),
                WorkItemHelper.getDefaultFields()));
    }

    public void createWorkItem() {
        if (latestServerContext != null && latestServerContext.getTeamProjectURI() != null) {
            final URI teamProjectURI = latestServerContext.getTeamProjectURI();
            if (teamProjectURI != null) {
                super.gotoLink(UrlHelper.getCreateWorkItemURI(teamProjectURI).toString());
            } else {
                logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
            }
        }
    }

    public void gotoMyWorkItems() {
        if (latestServerContext != null && latestServerContext.getTeamProjectURI() != null) {
            final URI teamProjectURI = latestServerContext.getTeamProjectURI();
            if (teamProjectURI != null) {
                super.gotoLink(UrlHelper.getCreateWorkItemURI(teamProjectURI).toString());
            } else {
                logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
            }
        }
    }

    public WorkItemsTableModel getTableModel() {
        return tableModel;
    }

    public ListSelectionModel getTableSelectionModel() {
        return tableModel.getSelectionModel();
    }

    public void setFilter(final String filter) {
        if (!StringUtils.equals(this.filter, filter)) {
            this.filter = filter;
            setChangedAndNotify(PROP_FILTER);
            tableModel.setFilter(filter);
        }
    }

    public String getFilter() {
        return filter;
    }

    public String getComment() {
        final ListSelectionModel selectionModel = getTableSelectionModel();
        if (!selectionModel.isSelectionEmpty()) {
            final StringBuilder sb = new StringBuilder();
            String separator = "";
            for (final WorkItem item : tableModel.getSelectedWorkItems()) {
                sb.append(separator);
                sb.append(WorkItemHelper.getWorkItemCommitMessage(item));
                separator = "\n";
            }
            return sb.toString();
        }

        return StringUtils.EMPTY;
    }

}

