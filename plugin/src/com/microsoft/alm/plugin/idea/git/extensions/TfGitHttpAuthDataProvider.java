// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.extensions;

import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.util.AuthData;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.ui.common.AzureDevOpsNotifications;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import git4idea.remote.GitHttpAuthDataProvider;
import git4idea.repo.GitRemote;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(TfGitHttpAuthDataProvider.class);

    /**
     * It is important that we override this method, because {@link GitHttpAuthDataProvider#getAuthData(String)}
     * override won't receive the `username@` as part of the URL starting from this version of IDEA. So we have to add
     * the username to the URL ourselves, because the internal Azure authentication mechanism requires the URL combined
     * with the username.
     */
    @Nullable
    @Override
    public AuthData getAuthData(@NotNull Project project, @NotNull String url, @NotNull String login) {
        logger.info("getAuthData: processing URL {}, login {}", url, login);
        try {
            String urlWithLogin = appendOrganizationInfo(url, login);
            return getAuthData(project, urlWithLogin);
        } catch (URISyntaxException e) {
            logger.warn("Error when parsing URL \"" + url + "\"", e);
            return getAuthData(url);
        }
    }

    @Nullable
    @Override
    public AuthData getAuthData(@NotNull Project project, @NotNull String url) {
        logger.info("getAuthData: processing URL {}", url);

        URI remoteUri = URI.create(url);
        String host = remoteUri.getHost();
        if (UrlHelper.isOrganizationHost(host)) {
            logger.info("getAuthData: is Azure DevOps host: {}", host);

            // For azure.com hosts (mainly for dev.azure.com), we need to check if the organization name could be
            // determined from the URL passed from Git. Sometimes, when using the Git repositories created in Visual
            // Studio, the organization name may be omitted from the authority part.
            //
            // Usually, a proper Git remote URL for dev.azure.com looks like this:
            // https://{organization}@dev.azure.com/{organization}/{repo}/_git/{repo}
            //
            // The reason for that is simple: when authenticating the user, Git will only pass the authority part (i.e.
            // https://{organization}@dev.azure.com) to the askpass program (and IDEA implements the askpass protocol
            // for Git), so, without the "{organization}@" part in the URL authority, it would be impossible to know in
            // which organization we should authenticate.
            //
            // If we're in the situation when we use dev.azure.com and the organization name is unknown from the
            // authority part of the URL, we may guess the organization by analyzing the Git remotes in the current
            // project.
            if (!Strings.isNullOrEmpty(remoteUri.getUserInfo())) {
                logger.info("getAuthData: URL has authentication info");
                return getAuthData(url);
            }

            Collection<GitRemote> remotes = TfGitHelper.getTfGitRemotes(project);
            List<String> organizationsFromRemotes = remotes.stream()
                    .map(GitRemote::getFirstUrl)
                    .filter(Objects::nonNull)
                    .map(URI::create)
                    .filter(uri -> host.equals(uri.getHost()))
                    .map(UrlHelper::getAccountFromOrganizationUri)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (organizationsFromRemotes.size() == 0) {
                logger.info("getAuthData: no Azure DevOps organizations detected");
                return null; // we cannot authenticate without knowing the organization
            }

            if (organizationsFromRemotes.size() > 1) {
                // If there's more that one Azure-like Git remote, then we have no information on which remote to use,
                // so we only could fail with notification.
                logger.info("getAuthData: more than one Azure DevOps organizations detected: {}", organizationsFromRemotes);
                AzureDevOpsNotifications.showManageRemoteUrlsNotification(project, host);
                return null;
            }

            String organizationName = organizationsFromRemotes.get(0);

            try {
                url = appendOrganizationInfo(url, organizationName);
            } catch (URISyntaxException e) {
                logger.warn("Error when parsing URL \"" + url + "\"", e);
            }
        }

        return getAuthData(url);
    }

    @Override
    public AuthData getAuthData(@NotNull String url) {
        logger.info("getAuthData: processing URL {}", url);

        url = UrlHelper.convertToCanonicalHttpApiBase(url);

        //try to find authentication info from saved server contexts
        final AuthenticationInfo authenticationInfo = ServerContextManager.getInstance().getBestAuthenticationInfo(url, false);
        if (authenticationInfo != null) {
            return new AuthData(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }

        //couldn't find authentication info from saved contexts
        logger.debug("getAuthData: Couldn't find authentication info from saved contexts.");

        if (UrlHelper.isTeamServicesUrl(url)) {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (UrlHelper.isOrganizationHost(host)) {
                // For dev.azure.com we need to check if the organization was included.
                logger.info("getAuthData: is Azure DevOps host: {}", host);
                if (Strings.isNullOrEmpty(UrlHelper.getAccountFromOrganizationUri(uri))) {
                    logger.warn("getAuthData: no user information detected");

                    // If we're at this point, it could only mean that we're in IDEA version older than 2018.2, so we
                    // have no Project and cannot determine the Git remotes in the project. Only thing we could do is
                    // show a notification and suggest something's wrong with the remotes.
                    AzureDevOpsNotifications.showManageRemoteUrlsNotification(null, host);
                    return null;
                }
            }

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

            return null;
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

    private static String appendOrganizationInfo(String url, String organization) throws URISyntaxException {
        return new URIBuilder(url).setUserInfo(organization).build().toString();
    }
}
