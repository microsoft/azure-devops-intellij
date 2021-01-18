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
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import git4idea.remote.GitHttpAuthDataProvider;
import git4idea.repo.GitRemote;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TfGitHttpAuthDataProvider implements GitHttpAuthDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(TfGitHttpAuthDataProvider.class);

    private static URI tryDetectApiUriFromGitRemotes(Project project, URI remoteUri) {
        String host = remoteUri.getHost();
        if (UrlHelper.isOrganizationHost(host)) {
            logger.info("tryDetectApiUriFromGitRemotes: is Azure DevOps host: {}", host);

            // For proper authentication on azure.com and dev.azure.com, we'll need to know the organization to
            // authenticate to. The organization is stored in the remote URL, but Git won't pass the full URL to the
            // askpass program (which IntelliJ implements), but will only pass the scheme and authority parts (including
            // the username@ part, if available). So, when trying to work with a canonical remote named
            // "https://{username}@dev.azure.com/{organization}/{repo}/_git/{repo}", the askpass program will only
            // receive "https://{username}@dev.azure.com".
            //
            // In most cases, "{username}" is the same as "{organization}", but in some cases it isn't (e.g. when
            // using an alternate credentials pair: in such case, "{username}" may be a username user chose as part of
            // the alternate credentials).
            //
            // So, we cannot reliably use the "{username}@" from the URL to determine the Azure DevOps organization, and
            // may only try guessing it by analyzing the Git remotes in the current project.
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
                logger.info("tryDetectApiUriFromGitRemotes: no Azure DevOps organizations detected");
                return null; // we cannot authenticate without knowing the organization
            }

            if (organizationsFromRemotes.size() > 1) {
                // If there's more that one Azure-like Git remote, then we have no information on which remote to use,
                // so we only could fail with notification.
                logger.info(
                        "tryDetectApiUriFromGitRemotes: more than one Azure DevOps organizations detected: {}",
                        organizationsFromRemotes);
                return null;
            }

            String organizationName = organizationsFromRemotes.get(0);

            URI result = UrlHelper.createOrganizationUri(host, organizationName);
            logger.info("Organization name appended to the URI: {}", result);
            return result;
        }

        return null;
    }

    @Nullable
    @Override
    public AuthData getAuthData(@NotNull Project project, @NotNull String url) {
        logger.info("getAuthData: init with URL {}", url);

        URI remoteUri = URI.create(url);
        URI apiBaseFromRemote = tryDetectApiUriFromGitRemotes(project, remoteUri);
        if (apiBaseFromRemote != null) {
            url = apiBaseFromRemote.toString();
            logger.info("getAuthData: URI override: {}", url);
        }

        logger.info("getAuthData: processing URL {}", url);

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
                    logger.warn("getAuthData: user information could not be determined from the project URL {}", uri);
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
    public void forgetPassword(@NotNull Project project, @NotNull String url, @NotNull AuthData authData) {
        // This method got called since stored credentials for the url resulted in an unauthorized error 401 or 403
        URI apiBaseFromRemote = tryDetectApiUriFromGitRemotes(project, URI.create(url));
        if (apiBaseFromRemote != null) {
            url = apiBaseFromRemote.toString();
            logger.info("forgetPassword: URI override: {}", url);
        }

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
