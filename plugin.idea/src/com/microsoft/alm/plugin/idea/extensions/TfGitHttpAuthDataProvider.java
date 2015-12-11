// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.extensions;

import com.intellij.util.AuthData;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import git4idea.remote.GitHttpAuthDataProvider;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    @Override
    public AuthData getAuthData(final String url) {
        assert url != null;
        //try to find authentication info from saved server contexts
        AuthenticationInfo authenticationInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(url, false);
        if(authenticationInfo == null && UrlHelper.isVSO(UrlHelper.getBaseUri(url))) {
            // Prompt for credentials if we know it is VSO
            // we can't determine if the url is for a TFS on premise server since IntelliJ Git plugin only calls us with the server URL (hostname)
            authenticationInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(url, true);
        }
        if (authenticationInfo != null) {
            return new AuthData(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }

        //Return null if we couldn't find matching git credentials
        //This will tell the Git plugin to prompt the user for credentials instead of failing silently with "Not authorized" error
        return null;
    }

    @Override
    public void forgetPassword(final String url) {
        // This method got called since stored credentials for the url resulted in an unauthorized error 401 or 403
        assert url != null;

        ServerContextManager.getInstance().updateAuthenticationInfo(url);
    }
}
