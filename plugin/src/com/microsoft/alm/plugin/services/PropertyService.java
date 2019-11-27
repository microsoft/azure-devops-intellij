// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import org.jetbrains.annotations.NotNull;

public interface PropertyService {
    String PROP_LAST_CONTEXT_KEY = "lastContextKey";
    String PROP_REPO_ROOT = "repoRoot";
    String PROP_TF_HOME = "tfHome";
    String PROP_AUTH_TYPE = "authType";
    String PROP_TF_SDK_EULA_ACCEPTED = "tfSdkEulaAccepted";
    String PROP_TFVC_USE_REACTIVE_CLIENT = "tfvcUseReactiveClient";

    @NotNull
    static PropertyService getInstance() {
        return PluginServiceProvider.getInstance().getPropertyService();
    }

    String getProperty(String propertyName);

    void setProperty(String propertyName, String value);

    void removeProperty(String propertyName);
}
