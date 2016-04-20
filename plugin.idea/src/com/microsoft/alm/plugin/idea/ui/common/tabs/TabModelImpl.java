// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.ui.common.FilteredModel;
import com.microsoft.alm.plugin.idea.ui.common.LookupListenerImpl;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.Operation;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TabModelImpl<T extends FilteredModel> extends AbstractModel implements TabModel {
    private static final Logger logger = LoggerFactory.getLogger(TabModelImpl.class);

    protected final Project project;
    protected final T viewForModel;
    protected LookupListenerImpl dataProvider;
    protected GitRepository gitRepository;
    private String filter;
    private String propStatus;
    private VcsTabStatus tabStatus = VcsTabStatus.NOT_TF_GIT_REPO;

    public TabModelImpl(@NotNull final Project project, @NotNull T viewModel, @NotNull final String propStatus) {
        this.project = project;
        this.viewForModel = viewModel;
        this.propStatus = propStatus;

        // need to create data provider after calling parent class since it passes the class to the provider
        createDataProvider();
    }

    /**
     * Creates data provider object based on child class
     */
    protected abstract void createDataProvider();

    public VcsTabStatus getTabStatus() {
        return tabStatus;
    }

    public void setTabStatus(final VcsTabStatus status) {
        if (this.tabStatus != status) {
            this.tabStatus = status;
            setChangedAndNotify(propStatus);
        }
    }

    public T getModelForView() {
        return viewForModel;
    }

    protected boolean isTfGitRepository() {
        gitRepository = TfGitHelper.getTfGitRepository(project);
        if (gitRepository == null) {
            setTabStatus(VcsTabStatus.NOT_TF_GIT_REPO);
            logger.debug("isTfGitRepository: Failed to get Git repo for current project");
            return false;
        } else {
            return true;
        }
    }

    public void loadData() {
        if (isTfGitRepository()) {
            clearData();
            dataProvider.loadData(TfGitHelper.getTfGitRemoteUrl(gitRepository));
        }
    }

    public void importIntoTeamServicesGit() {
        final ImportController controller = new ImportController(project);
        controller.showModalDialog();
    }

    public abstract void openSelectedItemsLink();

    public abstract void appendData(final Operation.Results results);

    protected abstract void clearData();

    public abstract void createNewItem();

    public void setFilter(final String filter) {
        if (!StringUtils.equals(this.filter, filter)) {
            this.filter = filter;
            setChangedAndNotify(PROP_FILTER);
            viewForModel.setFilter(filter);
        }
    }

    public String getFilter() {
        return filter;
    }

    public void dispose() {
        dataProvider.terminateActiveOperation();
    }
}