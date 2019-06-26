// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.microsoft.alm.plugin.context.ServerContext;

import java.util.List;

public interface ServerContextLookupPageModel {
    void setLoading(final boolean loading);

    boolean isLoading();

    void addError(final ModelValidationInfo validationInfo);

    void clearErrors();

    void appendContexts(final List<ServerContext> serverContexts);

    void clearContexts();
}
