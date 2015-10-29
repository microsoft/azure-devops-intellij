// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.mocks;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextLookupPageModel;

import java.util.ArrayList;
import java.util.List;

public class MockServerContextLookupPageModel implements ServerContextLookupPageModel {
    public boolean loading;
    public List<ModelValidationInfo> errors = new ArrayList<ModelValidationInfo>();
    public List<ServerContext> contexts = new ArrayList<ServerContext>();

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public void addError(ModelValidationInfo validationInfo) {
        errors.add(validationInfo);
    }

    @Override
    public void clearErrors() {
        errors.clear();
    }

    @Override
    public void appendContexts(List<ServerContext> serverContexts) {
        contexts.addAll(serverContexts);
    }

    @Override
    public void clearContexts() {
        contexts.clear();
    }
}
