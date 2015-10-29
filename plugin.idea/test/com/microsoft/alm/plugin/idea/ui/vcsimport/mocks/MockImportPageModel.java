// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportModel;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportPageModelImpl;
import org.apache.commons.lang.StringUtils;


public class MockImportPageModel extends ImportPageModelImpl {
    public final static String NO_ERRORS = "";
    private boolean importCalled;
    private boolean loadTeamProjectsCalled;
    private String validationError;
    private String urlVisited;

    public MockImportPageModel(ImportModel model, boolean showCollectionColumn) {
        super(model, showCollectionColumn ? ServerContextTableModel.TFS_PROJECT_COLUMNS : ServerContextTableModel.VSO_PROJECT_COLUMNS);
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return null;
    }

    @Override
    public void loadTeamProjects() {
        loadTeamProjectsCalled = true;

        setAuthenticating(false);
        setConnected(true);
        setLoading(false);
    }

    @Override
    public void importIntoRepository() {
        importCalled = true;
    }

    @Override
    public ModelValidationInfo validate() {
        if(validationError != null) {
            if(validationError.equals(NO_ERRORS)) {
                return null; //Don't call the default validation
            }
            return ModelValidationInfo.createWithMessage(validationError);
        }
        return super.validate();
    }

    @Override
    public void addContext(ServerContext context) {
        super.addContext(context);
    }

    @Override
    public void gotoLink(String url) {
        urlVisited = url;

        // allow empty strings to go through to base class for testing
        // skip non empty strings to avoid popping up browser window during tests
        if(StringUtils.isEmpty(url)) {
            super.gotoLink(url);
        }
    }

    public void initialize(ImportModel importModel) {
        setParentModel(importModel);
    }

    public boolean isImportCalled() {
        return importCalled;
    }

    public boolean isLoadTeamProjectsCalled() {
        return loadTeamProjectsCalled;
    }

    public void clearInternals() {
        importCalled = false;
        loadTeamProjectsCalled = false;
    }

    public void setValidationError(String error) {
        validationError = error;
    }

    public String getUrlVisited() {
        return urlVisited;
    }

}
