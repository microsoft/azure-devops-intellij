// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItem;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
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
    private String filter;
    private VcsTabStatus tabStatus = VcsTabStatus.NOT_TF_GIT_REPO;


    public final static String PROP_PR_WI_STATUS = "wiTabStatus";
    public final static String PROP_SERVER_NAME = "serverName";
    public final static String PROP_FILTER = "filter";


    public VcsWorkItemsModel(final @NotNull Project project) {
        this.project = project;

        tableModel = new WorkItemsTableModel(WorkItemsTableModel.DEFAULT_COLUMNS);
        treeDataProvider = new WorkItemsLookupListener(this, tableModel);
    }

    public VcsTabStatus getTabStatus() {
        return tabStatus;
    }

    public void setTabStatus(final VcsTabStatus status) {
        if (this.tabStatus != status) {
            this.tabStatus = status;
            setChangedAndNotify(PROP_PR_WI_STATUS);
        }
    }

    public WorkItemsTableModel getTableModel() {
        return tableModel;
    }

    private boolean isTfGitRepository() {
        gitRepository = TfGitHelper.getTfGitRepository(project);
        if (gitRepository == null) {
            setTabStatus(VcsTabStatus.NOT_TF_GIT_REPO);
            logger.debug("isTfGitRepository: Failed to get Git repo for current project");
            return false;
        } else {
            return true;
        }
    }

    public void loadWorkItems() {
        if (isTfGitRepository()) {
            clearWorkItems();
            treeDataProvider.loadWorkItems(TfGitHelper.getTfGitRemoteUrl(gitRepository));
        }
    }

    public void importIntoTeamServicesGit() {
        final ImportController controller = new ImportController(project);
        controller.showModalDialog();
    }

    public void openSelectedWorkItemsLink() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

            if (context != null && context.getTeamProjectURI() != null) {
                final List<WorkItem> workItems = tableModel.getSelectedWorkItems();
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

    public void clearWorkItems() {
        tableModel.clearRows();
    }

    public void createNewWorkItemLink() {
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

    public void dispose() {
        treeDataProvider.terminateActiveOperation();
    }
}