// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.authentication.facades.AuthenticationInfoCallback;
import com.microsoft.alm.plugin.authentication.facades.AuthenticationInfoProvider;
import com.microsoft.alm.plugin.authentication.facades.VsoAuthInfoProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this AuthenticationProvider to authenticate with VSO.
 */
public class VsoAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(VsoAuthenticationProvider.class);

    public static final String VSO_AUTH_URL = "https://app.vssps.visualstudio.com";

    private AuthenticationInfoProvider getAuthenticationInfoProvider() {
        return VsoAuthInfoProvider.getProvider();
    }

    /**
     * This constructor is protected to allow for testing
     */
    protected VsoAuthenticationProvider() {
    }

    private static class Holder {
        private static VsoAuthenticationProvider INSTANCE = new VsoAuthenticationProvider();
    }

    public static VsoAuthenticationProvider getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(final String serverUri) {
        final AuthenticationInfo authenticationInfo =
                ServerContextManager.getInstance().getBestAuthenticationInfo(serverUri, false);

        return authenticationInfo;
    }

    @Override
    public boolean isAuthenticated(final String serverUri) {
        return getAuthenticationInfo(serverUri) != null;
    }

    @Override
    public void clearAuthenticationDetails(final String serverUri) {
        ServerContextManager.getInstance().remove(serverUri);
        getAuthenticationInfoProvider().clearAuthenticationInfo(serverUri);
    }

    @Override
    public void authenticateAsync(final String serverUri, final AuthenticationListener listener) {
        logger.info("authenticateAsync for server: {}", serverUri);
        //clear in memory cache, otherwise user is not prompted, TOOD: handle in auth library since it can be an issue for TFS also
        getAuthenticationInfoProvider().clearAuthenticationInfo(VSO_AUTH_URL);

        AuthenticationListener.Helper.authenticating(listener);

        getAuthenticationInfoProvider().getAuthenticationInfoAsync(serverUri, new AuthenticationInfoCallback() {
            @Override
            public void onSuccess(final AuthenticationInfo authenticationInfo) {
                logger.info("getAuthenticationInfoAsync succeeded");
                try {
                    //save for VSO_Deployment
                    if (StringUtils.equalsIgnoreCase(serverUri, VSO_AUTH_URL)) {
                        ServerContextManager.getInstance().validateServerConnection(
                                new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT)
                                        .uri(VSO_AUTH_URL)
                                        .authentication(authenticationInfo)
                                        .build());
                    } else {
                        //save for the specific server url
                        ServerContextManager.getInstance().validateServerConnection(
                                new ServerContextBuilder().type(ServerContext.Type.VSO)
                                        .uri(serverUri)
                                        .authentication(authenticationInfo)
                                        .build());
                    }

                    //success
                    AuthenticationListener.Helper.authenticated(listener, authenticationInfo, null);
                } catch (Throwable t) {
                    //validate server connection can fail if we are unable to parse the Url
                    AuthenticationListener.Helper.authenticated(listener, AuthenticationInfo.NONE, t);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error("getAuthenticationInfoAsync failed", t);
                clearAuthenticationDetails(serverUri);
                AuthenticationListener.Helper.authenticated(listener, AuthenticationInfo.NONE, t);
            }
        });
    }
}
