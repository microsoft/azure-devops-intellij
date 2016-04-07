// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.extensions;

import com.intellij.util.AuthData;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContextManager;
import git4idea.remote.GitHttpAuthDataProvider;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    @Override
    public AuthData getAuthData(final String url) {
        assert url != null;

        //try to find authentication info from saved server contexts
        final AuthenticationInfo authenticationInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(url, false);
        if (authenticationInfo != null) {
            return new AuthData(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }

        //couldn't find authentication info from saved contexts
        if (UrlHelper.isTeamServicesUrl(url)) {
            // We can't determine if the url is for a TFS on premise server but prompt for credentials if we know it is VSO
            final AuthenticationInfo vsoAuthenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(VsoAuthenticationProvider.getInstance(), url);
            if (vsoAuthenticationInfo == null) {
                //user cancelled authentication, send empty credentials to cause a auth failure
                return new AuthData("", "");
            } else {
                return new AuthData(vsoAuthenticationInfo.getUserName(), vsoAuthenticationInfo.getPassword());
            }
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
