// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

public interface PropertyService {
    String PROP_LAST_CONTEXT_KEY = "lastContextKey";
    String PROP_REACTIVE_CLIENT_PATH = "reactiveClientPath";
    String PROP_REPO_ROOT = "repoRoot";
    String PROP_TF_HOME = "tfHome";
    String PROP_AUTH_TYPE = "authType";

    String getProperty(String propertyName);

    void setProperty(String propertyName, String value);

    void removeProperty(String propertyName);
}
