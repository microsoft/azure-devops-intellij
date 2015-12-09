// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.extensions;

import com.intellij.util.AuthData;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContextManager;
import git4idea.remote.GitHttpAuthDataProvider;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    @Override
    public AuthData getAuthData(final String url) {
        assert url != null;
        final AuthenticationInfo authenticationInfo = ServerContextManager.getInstance().getAuthenticationInfo(url, true);
        if (authenticationInfo != null) {
            return new AuthData(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }

        //Return null if we couldn't find matching git credentials
        //This will tell the Git plugin to prompt the user for credentials instead of failing silently with "Not authorized" error
        return null;
    }

    @Override
    public void forgetPassword(final String url) {
        assert url != null;
        ServerContextManager.getInstance().remove(url);
    }
}
