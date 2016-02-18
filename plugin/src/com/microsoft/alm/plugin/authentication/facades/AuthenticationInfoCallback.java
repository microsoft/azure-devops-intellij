// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;

public interface AuthenticationInfoCallback extends FutureCallback<AuthenticationInfo> {

    void onSuccess(final AuthenticationInfo info);

    void onFailure(final Throwable t);
}
