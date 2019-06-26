// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.util.AuthData;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContextManager;
import git4idea.remote.GitHttpAuthDataProvider;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(TfGitHttpAuthDataProvider.class);

    /**
     * This is a method that was introduced into {@link GitHttpAuthDataProvider} in IDEA 2018.2. Azure DevOps plugin is
     * still compatible with IDEA 2017, thus we cannot properly mark this method as @Override.
     *
     * It is important that we override this method in IDEA 2018.2+, because
     * {@link GitHttpAuthDataProvider#getAuthData(String)} override won't receive the `username@` as part of the URL
     * starting from this version of IDEA. So we have to add the username to the URL ourselves, because the internal
     * Azure authentication mechanism requires the URL combined with the username.
     *
     * Despite not marked as @Override, this method still overrides the interface method in IDEA 2018.2+, because this
     * is how Java ABI works.
     */
    @Nullable
    // @Override // HACK: It is impossible to mark this method as @Override according to the above.
    public AuthData getAuthData(@NotNull Project project, @NotNull String url, @NotNull String login) {
        try {
            String urlWithLogin = new URIBuilder(url).setUserInfo(login).build().toString();
            return getAuthData(urlWithLogin);
        } catch (URISyntaxException e) {
            logger.warn("Error when parsing URL \"" + url + "\"", e);
            return getAuthData(url);
        }
    }

    @Override
    public AuthData getAuthData(@NotNull String url) {
        url = UrlHelper.convertToCanonicalHttpApiBase(url);

        //try to find authentication info from saved server contexts
        final AuthenticationInfo authenticationInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(url, false);
        if (authenticationInfo != null) {
            return new AuthData(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }

        //couldn't find authentication info from saved contexts
        logger.debug("getAuthData: Couldn't find authentication info from saved contexts.");

        if (UrlHelper.isTeamServicesUrl(url)) {
            // We can't determine if the url is for a TFS on premise server but prompt for credentials if we know it is VSO
            // IntelliJ calls us with a http server url e.g. http://myorganization.visualstudio.com
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
                logger.warn("getAuthData: Unable to get https Azure DevOps Services url for input url = " + url);
            }
        }

        //Return null if we couldn't find matching git credentials
        //This will tell the Git plugin to prompt the user for credentials instead of failing silently with "Not authorized" error
        return null;
    }

    @Override
    public void forgetPassword(@NotNull String url) {
        // This method got called since stored credentials for the url resulted in an unauthorized error 401 or 403
        url = UrlHelper.convertToCanonicalHttpApiBase(url);

        if (UrlHelper.isTeamServicesUrl(url)) {
            // IntelliJ calls us with a http server url e.g. http://myorganization.visualstudio.com
            // convert to https:// for team services to avoid rest call failures
            final String authUrl = UrlHelper.getHttpsUrlFromHttpUrl(url);
            if (authUrl != null) {
                ServerContextManager.getInstance().updateAuthenticationInfo(authUrl);
            } else {
                logger.warn("forgetPassword: Unable to get https Azure DevOps Services url for input url = " + url);
            }
        } else {
            //if onprem server is https:// we can't detect it, just use url provided by intelliJ
            logger.warn("forgetPassword: If server is https:// user might see multiple prompts for entering password resulting in all failures.");
            ServerContextManager.getInstance().updateAuthenticationInfo(url);
        }
    }
}
