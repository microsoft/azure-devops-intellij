// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

public interface AuthenticationInfoProvider {

    void getAuthenticationInfoAsync(final String serverUri, final AuthenticationInfoCallback callback);

}
