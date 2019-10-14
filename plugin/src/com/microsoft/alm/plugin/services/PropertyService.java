// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.alm.plugin.idea.tfvc.core.ClassicTfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.ReactiveTfvcClient;
import org.jetbrains.annotations.NotNull;

public interface PropertyService {
    String PROP_LAST_CONTEXT_KEY = "lastContextKey";
    String PROP_REACTIVE_CLIENT_PATH = "reactiveClientPath";
    String PROP_TFVC_CLIENT_TYPE = "tfvcClientType";
    String PROP_REPO_ROOT = "repoRoot";
    String PROP_TF_HOME = "tfHome";
    String PROP_AUTH_TYPE = "authType";

    String CLIENT_TYPE_CLASSIC = ClassicTfvcClient.class.getName();
    String CLIENT_TYPE_REACTIVE = ReactiveTfvcClient.class.getName();

    @NotNull
    static PropertyService getInstance() {
        return PluginServiceProvider.getInstance().getPropertyService();
    }

    String getProperty(String propertyName);

    void setProperty(String propertyName, String value);

    void removeProperty(String propertyName);
}
