// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.plugin.idea.ui.common.LoginPageModel;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;

import javax.swing.ListSelectionModel;

public interface ImportPageModel extends LoginPageModel {
    String PROP_LOADING = "loading";
    String PROP_PROJECT_FILTER = "teamProjectFilter";
    String PROP_REPO_NAME = "repositoryName";
    String PROP_PROJECT_TABLE = "teamProjectTable";

    String getRepositoryName();

    void setRepositoryName(final String repositoryName);

    String getTeamProjectFilter();

    void setTeamProjectFilter(final String teamProjectFilter);

    boolean isLoading();

    void setLoading(final boolean loading);

    void setImportEnabled(final boolean importEnabled);

    ServerContextTableModel getTableModel();

    ListSelectionModel getTableSelectionModel();

    void loadTeamProjects();

    void importIntoRepository();
}
