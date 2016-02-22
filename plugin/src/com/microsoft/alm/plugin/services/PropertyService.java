// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

public interface PropertyService {
    String PROP_LAST_CONTEXT_KEY = "lastContextKey";
    String PROP_REPO_ROOT = "repoRoot";
    String PROP_USE_JAVAFX_AUTH_LIBRARY = "useJavaFxAuthLibrary";

    String getProperty(String propertyName);

    void setProperty(String propertyName, String value);
}
