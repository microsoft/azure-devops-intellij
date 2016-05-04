// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabModelImpl;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class VcsWorkItemsModel extends TabModelImpl<WorkItemsTableModel> {
    private static final Logger logger = LoggerFactory.getLogger(VcsWorkItemsModel.class);

    public VcsWorkItemsModel(final @NotNull Project project) {
        super(project, new WorkItemsTableModel(WorkItemsTableModel.COLUMNS_PLUS_BRANCH));
    }

    protected void createDataProvider() {
        dataProvider = new WorkItemsTabLookupListener(this);
    }

    public void openGitRepoLink() {
        // create a new work item and open WIT takes you to the same place at the moment
        createNewItem();
    }

    public void openSelectedItemsLink() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

            if (context != null && context.getTeamProjectURI() != null) {
                final List<WorkItem> workItems = viewForModel.getSelectedWorkItems();
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
    }

    public void appendData(final Operation.Results results) {
        final WorkItemLookupOperation.WitResults witResults = (WorkItemLookupOperation.WitResults) results;
        viewForModel.addWorkItems(witResults.getWorkItems());
    }

    public void clearData() {
        viewForModel.clearRows();
    }

    public void createNewItem() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

            if (context != null && context.getTeamProjectURI() != null) {
                final URI teamProjectURI = context.getTeamProjectURI();
                if (teamProjectURI != null) {
                    super.gotoLink(UrlHelper.getCreateWorkItemURI(teamProjectURI).toString());
                } else {
                    logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
                }
            }
        }
    }
}