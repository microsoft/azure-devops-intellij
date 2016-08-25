// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.AzureAuthority;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.oauth2.useragent.AuthorizationException;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.exceptions.ProfileDoesNotExistException;
import com.microsoft.alm.plugin.services.DeviceFlowResponsePrompt;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.provider.JaxrsClientProvider;
import com.microsoft.alm.secret.Token;
import com.microsoft.alm.secret.TokenPair;
import com.microsoft.alm.secret.VsoTokenScope;
import com.microsoft.alm.storage.InsecureInMemoryStore;
import com.microsoft.alm.storage.SecretStore;
import com.microsoft.visualstudio.services.account.AccountHttpClient;
import com.microsoft.visualstudio.services.account.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class VsoAuthInfoProvider implements AuthenticationInfoProvider {
    private final static Logger logger = LoggerFactory.getLogger(VsoAuthInfoProvider.class);

    private static final String CLIENT_ID = "97877f11-0fc6-4aee-b1ff-febb0519dd00";
    private static final String REDIRECT_URL = "https://java.visualstudio.com";

    private final ListeningExecutorService executorService;
    private final SecretStore<TokenPair> accessTokenStore;
    private final SecretStore<Token> tokenStore;
    private final DeviceFlowResponsePrompt deviceFlowResponsePrompt;
    private final VstsPatAuthenticator vstsPatAuthenticator;

    private VsoAuthInfoProvider() {
        accessTokenStore = new InsecureInMemoryStore<TokenPair>();
        tokenStore = new InsecureInMemoryStore<Token>();

        //Only allow 5 threads to start polling
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));

        deviceFlowResponsePrompt = PluginServiceProvider.getInstance().getDeviceFlowResponsePrompt();

        vstsPatAuthenticator = new VstsPatAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore, tokenStore);
    }

    private static class VsoJavaFxExperimentalAuthInfoProviderHolder {
        public static VsoAuthInfoProvider INSTANCE = new VsoAuthInfoProvider();
    }

    public static VsoAuthInfoProvider getProvider() {
        return VsoJavaFxExperimentalAuthInfoProviderHolder.INSTANCE;
    }

    @Override
    public void getAuthenticationInfoAsync(final String serverUri, final AuthenticationInfoCallback callback) {
        final SettableFuture<AuthenticationInfo> authenticationInfoFuture = SettableFuture.<AuthenticationInfo>create();

        // Callback for the Device Flow dialog to cancel the current authenticating process.
        // Normally this is hooked up to the cancel button so if user cancels, we do not wait forever in
        // a polling loop.
        final Action<String> cancellationCallback = new Action<String>() {
            @Override
            public void call(final String reasonForCancel) {
                authenticationInfoFuture.setException(new AuthorizationException(reasonForCancel));
            }
        };

        // Must share the same accessTokenStore with the member variable vstsPatAuthenticator to avoid prompt the user
        // when we generate PAT
        final OAuth2Authenticator.OAuth2AuthenticatorBuilder oAuth2AuthenticatorBuilder = new OAuth2Authenticator.OAuth2AuthenticatorBuilder()
                .withClientId(CLIENT_ID)
                .redirectTo(REDIRECT_URL)
                .backedBy(accessTokenStore)
                .withDeviceFlowCallback(deviceFlowResponsePrompt.getCallback(cancellationCallback));

        // Check if common url was passed or if a specific url was given
        // If a specific url is being used and a tenant id is found use the tenant id with the authenticator
        String resourceId = OAuth2Authenticator.MANAGEMENT_CORE_RESOURCE;
        if (!OAuth2Authenticator.APP_VSSPS_VISUALSTUDIO.getAuthority().equals(URI.create(serverUri).getAuthority())) {
            try {
                final UUID tenantId = AzureAuthority.detectTenantId(URI.create(serverUri));
                if (tenantId != null) {
                    logger.info(String.format("Adding tenant id %s to oAuth2Authenticator builder for url %s",
                            tenantId.toString(), serverUri));
                    resourceId = OAuth2Authenticator.VSTS_RESOURCE;
                    oAuth2AuthenticatorBuilder.withTenantId(tenantId);
                }
            } catch (Error e) {
                logger.warn("Error while trying to get tenant id", e);
                // ok to continue without using tenant id
            }
        }
        oAuth2AuthenticatorBuilder.manage(resourceId);

        final OAuth2Authenticator oAuth2Authenticator = oAuth2AuthenticatorBuilder.build();
        final JaxrsClientProvider jaxrsClientProvider = new JaxrsClientProvider(oAuth2Authenticator);

        try {
            AuthenticationInfo authenticationInfo = null;
            String errorMessage = null;
            final Client client = jaxrsClientProvider.getClient();

            if (client != null) {
                //Or we could reconsider the name of the token.  Now we call Profile endpoint just to get the email address
                //which is used in token description, but do we need it?  User can only view PATs after they login, and
                //at that time user knows which account/email they are logged in under already.  So the email provides
                //no additional value.
                final AccountHttpClient accountHttpClient
                        = new MyHttpClient(client, OAuth2Authenticator.APP_VSSPS_VISUALSTUDIO);

                final Profile me = accountHttpClient.getMyProfile();
                final String emailAddress = me.getCoreAttributes().getEmailAddress().getValue();

                final String tokenDescription = AuthHelper.getTokenDescription(emailAddress);

                final Token token = vstsPatAuthenticator.getPersonalAccessToken(
                        VsoTokenScope.AllScopes,
                        tokenDescription,
                        PromptBehavior.AUTO);

                if (token != null) {
                    authenticationInfo = new AuthenticationInfo(me.getId().toString(),
                            token.Value, serverUri, emailAddress);
                } else {
                    errorMessage = "Failed to get a Personal Access Token";
                }
            } else {
                errorMessage = "Failed to get authenticated jaxrs client.";
            }

            if (authenticationInfo != null) {
                authenticationInfoFuture.set(authenticationInfo);
            } else {
                authenticationInfoFuture.setException(new AuthorizationException(errorMessage));
            }

        } catch (Throwable t) {
            authenticationInfoFuture.setException(t);
        }

        Futures.addCallback(authenticationInfoFuture, callback);
    }

    @Override
    public void clearAuthenticationInfo(final String serverUri) {
        // Only generates global PAT currently, so ignore serverUri and clear global PAT only
        this.vstsPatAuthenticator.signOut();
    }

    // We are subclassing the AccountHttpClient here in order to add mapped exceptions
    // TODO: add handling in the TFS Java REST SDK for these exceptions
    private class MyHttpClient extends AccountHttpClient {

        public MyHttpClient(final Client jaxrsClient, final URI baseUrl) {
            super(jaxrsClient, baseUrl);
        }

        @Override
        protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
            final Map<String, Class<? extends Exception>> map = new HashMap<String, Class<? extends Exception>>();
            map.put("ProfileDoesNotExistException", ProfileDoesNotExistException.class);
            return map;
        }
    }
}
