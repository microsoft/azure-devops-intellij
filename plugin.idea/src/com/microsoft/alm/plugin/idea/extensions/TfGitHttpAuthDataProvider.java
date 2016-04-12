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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(TfGitHttpAuthDataProvider.class);

    @Override
    public AuthData getAuthData(final String url) {
        assert url != null;

        //try to find authentication info from saved server contexts
        final AuthenticationInfo authenticationInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(url, false);
        if (authenticationInfo != null) {
            return new AuthData(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }

        //couldn't find authentication info from saved contexts
        logger.debug("getAuthData: Couldn't find authentication info from saved contexts.");

        if (UrlHelper.isTeamServicesUrl(url)) {
            // We can't determine if the url is for a TFS on premise server but prompt for credentials if we know it is VSO
            // IntelliJ calls us with a http server url e.g. http://myaccount.visualstudio.com
            // convert to https:// for team services to avoid rest call failures
            final String authUrl = UrlHelper.getHttpsUrlFromHttpUrl(url);

            if (authUrl != null) {
                final AuthenticationInfo vsoAuthenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(VsoAuthenticationProvider.getInstance(), authUrl);
                if (vsoAuthenticationInfo == null) {
                    //user cancelled authentication, send empty credentials to cause a auth failure
                    return new AuthData("", "");
                } else {
                    return new AuthData(vsoAuthenticationInfo.getUserName(), vsoAuthenticationInfo.getPassword());
                }
            } else {
                logger.warn("getAuthData: Unable to get https team services url for input url = " + url);
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
        if (UrlHelper.isTeamServicesUrl(url)) {
            // IntelliJ calls us with a http server url e.g. http://myaccount.visualstudio.com
            // convert to https:// for team services to avoid rest call failures
            final String authUrl = UrlHelper.getHttpsUrlFromHttpUrl(url);
            if (authUrl != null) {
                ServerContextManager.getInstance().updateAuthenticationInfo(authUrl);
            } else {
                logger.warn("forgetPassword: Unable to get https team services url for input url = " + url);
            }
        } else {
            //if onprem server is https:// we can't detect it, just use url provided by intelliJ
            logger.warn("forgetPassword: If server is https:// user might see multiple prompts for entering password resulting in all failures.");
            ServerContextManager.getInstance().updateAuthenticationInfo(url);
        }
    }
}
