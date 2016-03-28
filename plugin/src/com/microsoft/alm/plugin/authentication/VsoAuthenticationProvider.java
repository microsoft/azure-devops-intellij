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
    public AuthenticationInfo getAuthenticationInfo() {
        return ServerContextManager.getInstance().getBestAuthenticationInfo(VSO_AUTH_URL, false);
    }

    @Override
    public boolean isAuthenticated() {
        return getAuthenticationInfo() != null;
    }

    @Override
    public void clearAuthenticationDetails() {
        ServerContextManager.getInstance().remove(VSO_AUTH_URL);
    }

    @Override
    public void authenticateAsync(final String serverUri, final AuthenticationListener listener) {
        AuthenticationListener.Helper.authenticating(listener);

        getAuthenticationInfoProvider().getAuthenticationInfoAsync(serverUri, new AuthenticationInfoCallback() {
            @Override
            public void onSuccess(final AuthenticationInfo authenticationInfo) {
                //save for VSO_Deployment
                ServerContextManager.getInstance().validateServerConnection(
                        new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT)
                                .uri(VSO_AUTH_URL)
                                .authentication(authenticationInfo)
                                .build());

                if (!StringUtils.equalsIgnoreCase(serverUri, VSO_AUTH_URL)) {
                    //save for the specific server url
                    ServerContextManager.getInstance().validateServerConnection(
                            new ServerContextBuilder().type(ServerContext.Type.VSO)
                                    .uri(serverUri)
                                    .authentication(authenticationInfo)
                                    .build());
                }

                //success
                AuthenticationListener.Helper.authenticated(listener, authenticationInfo, null);
            }

            @Override
            public void onFailure(Throwable t) {
                clearAuthenticationDetails();
                AuthenticationListener.Helper.authenticated(listener, AuthenticationInfo.NONE, t);
            }
        });
    }
}
