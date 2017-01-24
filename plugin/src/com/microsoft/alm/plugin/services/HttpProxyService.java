// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

/**
 * This service indicates if an http proxy is being used and provides authentication details if needed.
 */
public interface HttpProxyService {
    boolean useHttpProxy();
    boolean isAuthenticationRequired();
    String getProxyURL();
    String getProxyHost();
    int getProxyPort();
    String getUserName();
    String getPassword();
}
