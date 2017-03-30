// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.client.model.VssServiceResponseException;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Static helpers for Authentication
 */
public class AuthHelper {
    private final static Logger logger = LoggerFactory.getLogger(AuthHelper.class);

    /**
     * Personal Access Token description string formatter
     */
    private static final String TOKEN_DESCRIPTION_FORMATTER = "VSTS IntelliJ Plugin: %s from: %s on: %s";

    public static AuthenticationInfo createAuthenticationInfo(final String serverUri,
                                                              final Credentials credentials,
                                                              AuthenticationInfo.CredsType type) {
        return new AuthenticationInfo(
                credentials.getUserPrincipal().getName(),
                credentials.getPassword(),
                serverUri,
                credentials.getUserPrincipal().getName(),
                type,
                null
        );
    }

    /**
     * This method wraps the normal Async call to authenticate and waits on the result.
     */
    public static AuthenticationInfo getAuthenticationInfoSynchronously(final AuthenticationProvider provider, final String gitRemoteUrl) {
        logger.info("getAuthenticationInfoSynchronously calling authenticateAsync on " + gitRemoteUrl);
        final SettableFuture<AuthenticationInfo> future = SettableFuture.create();

        provider.authenticateAsync(gitRemoteUrl, new AuthenticationListener() {
            @Override
            public void authenticating() {
                // do nothing
            }

            @Override
            public void authenticated(final AuthenticationInfo authenticationInfo, final Throwable throwable) {
                if (throwable != null) {
                    future.setException(throwable);
                } else {
                    future.set(authenticationInfo);
                }
            }
        });

        // Wait for the authentication info object to be ready
        // Don't wait any longer than 15 minutes for the user to authenticate
        Throwable t = null;
        try {
            return future.get(15, TimeUnit.MINUTES);
        } catch (InterruptedException ie) {
            t = ie;
        } catch (ExecutionException ee) {
            t = ee;
        } catch (TimeoutException te) {
            t = te;
        } finally {
            if (t != null) {
                logger.error("getAuthenticationInfoSynchronously: failed to get authentication info from user");
                logger.warn("getAuthenticationInfoSynchronously", t);
            }
        }
        return null;
    }

    /**
     * Returns the NTCredentials or UsernamePasswordCredentials object
     *
     * @param type
     * @param authenticationInfo
     * @return
     */
    public static Credentials getCredentials(final ServerContext.Type type, final AuthenticationInfo authenticationInfo) {
        if (type == ServerContext.Type.TFS) {
            return getNTCredentials(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        } else {
            return new UsernamePasswordCredentials(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }
    }

    /**
     * Returns an NTCredentials object for given username and password
     *
     * @param userName
     * @param password
     * @return
     */
    public static NTCredentials getNTCredentials(final String userName, final String password) {
        assert userName != null;
        assert password != null;

        String user = userName;
        String domain = "";
        final String workstation = SystemHelper.getComputerName();

        // If the username has a backslash, then the domain is the first part and the username is the second part
        if (userName.contains("\\")) {
            String[] parts = userName.split("[\\\\]");
            if (parts.length == 2) {
                domain = parts[0];
                user = parts[1];
            }
        } else if (userName.contains("/")) {
            // If the username has a slash, then the domain is the first part and the username is the second part
            String[] parts = userName.split("[/]");
            if (parts.length == 2) {
                domain = parts[0];
                user = parts[1];
            }
        } else if (userName.contains("@")) {
            // If the username has an asterisk, then the domain is the second part and the username is the first part
            String[] parts = userName.split("[@]");
            if (parts.length == 2) {
                user = parts[0];
                domain = parts[1];
            }
        }

        return new org.apache.http.auth.NTCredentials(user, password, workstation, domain);
    }

    public static String getTokenDescription(final String emailAddress) {
        final String tokenDescription = String.format(TOKEN_DESCRIPTION_FORMATTER,
                emailAddress, SystemHelper.getComputerName(), new Date().toString());

        return tokenDescription;
    }

    public static boolean isNotAuthorizedError(final Throwable throwable) {
        //We get VssServiceResponseException when token is valid but does not have the required scopes
        //statusCode on VssServiceResponseException is set to 401 but that is not accessible, so we have to check the message
        //If the message gets localized, we won't detect the auth error
        if (throwable != null && (throwable instanceof NotAuthorizedException ||
                (throwable instanceof VssServiceResponseException &&
                        StringUtils.containsIgnoreCase(throwable.getMessage(), "unauthorized")))) {
            return true;
        }

        if (throwable != null && throwable.getCause() != null && (throwable.getCause() instanceof NotAuthorizedException ||
                (throwable.getCause() instanceof VssServiceResponseException &&
                        (StringUtils.containsIgnoreCase(throwable.getMessage(), "unauthorized"))))) {
            return true;
        }

        return false;
    }
}
