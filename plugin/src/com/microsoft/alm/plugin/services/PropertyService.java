// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import org.jetbrains.annotations.NotNull;

public interface PropertyService {
    String PROP_LAST_CONTEXT_KEY = "lastContextKey";
    String PROP_REPO_ROOT = "repoRoot";
    String PROP_TF_HOME = "tfHome";
    String PROP_AUTH_TYPE = "authType";
    String PROP_REACTIVE_CLIENT_MEMORY = "reactiveClientMemory";
    String PROP_TF_SDK_EULA_ACCEPTED = "tfSdkEulaAccepted";
    String PROP_TFVC_USE_REACTIVE_CLIENT = "tfvcUseReactiveClientV2";
    String PROP_VISUAL_STUDIO_TF_CLIENT_PATH = "visualStudioTfClientPath";

    @NotNull
    static PropertyService getInstance() {
        return PluginServiceProvider.getInstance().getPropertyService();
    }

    String getProperty(String propertyName);

    void setProperty(String propertyName, String value);

    void removeProperty(String propertyName);

    default boolean useReactiveClient() {
        String value = getProperty(PROP_TFVC_USE_REACTIVE_CLIENT);
        return value == null || "true".equalsIgnoreCase(value);
    }

    default void setUseReactiveClient(boolean value) {
        setProperty(PROP_TFVC_USE_REACTIVE_CLIENT, Boolean.toString(value));
    }
}
