// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.plugin.idea.common.ui.common.LoginPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;

import javax.swing.ListSelectionModel;

/**
 * This interface represents the model for either the VSO or TFS "checkout from version control" pages.
 * There is a base class implementation of this interface thru CheckoutPageModelImpl.
 */
public interface CheckoutPageModel extends LoginPageModel {
    String PROP_DIRECTORY_NAME = "directoryName";
    String PROP_LOADING = "loading";
    String PROP_ADVANCED = "advanced";
    String PROP_PARENT_DIR = "parentDirectory";
    String PROP_REPO_FILTER = "repositoryFilter";
    String PROP_REPO_TABLE = "repoTable";
    String PROP_TFVC_SERVER_CHECKOUT = "tfvcServerCheckout";

    String DEFAULT_SOURCE_PATH = System.getProperty("user.home");

    String getParentDirectory();

    void setParentDirectory(String parentDirectory);

    String getDirectoryName();

    void setDirectoryName(String directoryName);

    String getRepositoryFilter();

    void setRepositoryFilter(String repositoryFilter);

    boolean isLoading();

    void setAdvanced(boolean advanced);

    boolean isAdvanced();

    void setTfvcServerCheckout(boolean isTfvcServerCheckout);

    void setLoading(boolean loading);

    void setCloneEnabled(boolean cloneEnabled);

    ServerContextTableModel getTableModel();

    ListSelectionModel getTableSelectionModel();

    void loadRepositories();

    void cloneSelectedRepo();

}
