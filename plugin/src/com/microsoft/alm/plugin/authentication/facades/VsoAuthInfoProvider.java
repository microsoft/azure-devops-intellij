// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.oauth2.useragent.AuthorizationException;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo.CredsType;
import com.microsoft.alm.plugin.context.RestClientHelper;
import com.microsoft.alm.plugin.exceptions.ProfileDoesNotExistException;
import com.microsoft.alm.plugin.services.DeviceFlowResponsePrompt;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
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

public class VsoAuthInfoProvider implements AuthenticationInfoProvider {
    private final static Logger logger = LoggerFactory.getLogger(VsoAuthInfoProvider.class);

    private static final String CLIENT_ID = "97877f11-0fc6-4aee-b1ff-febb0519dd00";
    private static final String REDIRECT_URL = "https://java.visualstudio.com";

    private final SecretStore<TokenPair> accessTokenStore;
    private final SecretStore<Token> tokenStore;
    private final DeviceFlowResponsePrompt deviceFlowResponsePrompt;
    private final VstsPatAuthenticator vstsPatAuthenticator;

    private VsoAuthInfoProvider() {
        accessTokenStore = new InsecureInMemoryStore<TokenPair>();
        tokenStore = new InsecureInMemoryStore<Token>();

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
    public void getAuthenticationInfoAsync(String serverUri, final AuthenticationInfoCallback callback) {
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
        // when we generate PersonalAccessToken
        final OAuth2Authenticator.OAuth2AuthenticatorBuilder oAuth2AuthenticatorBuilder = new OAuth2Authenticator.OAuth2AuthenticatorBuilder()
                .withClientId(CLIENT_ID)
                .redirectTo(REDIRECT_URL)
                .backedBy(accessTokenStore)
                .manage(OAuth2Authenticator.MANAGEMENT_CORE_RESOURCE)
                .withDeviceFlowCallback(deviceFlowResponsePrompt.getCallback(cancellationCallback));
        final OAuth2Authenticator oAuth2Authenticator = oAuth2AuthenticatorBuilder.build();

        final URI encodedServerUri = UrlHelper.createUri(serverUri);
        final TokenPair tokenPair = oAuth2Authenticator.getOAuth2TokenPair(encodedServerUri, PromptBehavior.AUTO);

        try {
            AuthenticationInfo authenticationInfo = null;
            String errorMessage = null;

            authenticationInfo = getAuthenticationInfo(encodedServerUri, tokenPair);

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

    public AuthenticationInfo getAuthenticationInfo(final URI serverUri, final TokenPair tokenPair) {

        if (tokenPair != null) {
            final Client client = RestClientHelper.getClient(serverUri.toString(), tokenPair.AccessToken.Value);

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

                if (serverUri.equals(OAuth2Authenticator.APP_VSSPS_VISUALSTUDIO)) {
                    logger.debug("Creating authenticationInfo backed by AccessToken for: {}", serverUri);
                    return new AuthenticationInfo (
                            me.getId().toString(),
                            tokenPair.AccessToken.Value,
                            serverUri.toString(),
                            emailAddress,
                            CredsType.AccessToken,
                            tokenPair.RefreshToken.Value);
                } else {
                    logger.debug("Getting a PersonalAccessToken for: {}", serverUri);
                    final Token token = vstsPatAuthenticator.getPersonalAccessToken(
                            serverUri,
                            VsoTokenScope.AllScopes,
                            tokenDescription,
                            PromptBehavior.AUTO,
                            tokenPair);

                    if (token != null) {
                        logger.debug("Creating authenticationInfo backed by PersonalAccessToken for: {}", serverUri);
                        return new AuthenticationInfo(
                                me.getId().toString(),
                                token.Value,
                                serverUri.toString(),
                                emailAddress,
                                CredsType.PersonalAccessToken,
                                null);
                    } else {
                        logger.warn("Failed to get a Personal Access Token");
                    }
                }
            } else {
                logger.warn("Failed to get authenticated jaxrs client.");
            }
        } else {
            logger.warn("Failed to get AuthenticationInfo because AccessToken is null.");
        }

        return null;
    }

    @Override
    public void clearAuthenticationInfo(final String serverUri) {
        this.vstsPatAuthenticator.signOut(URI.create(serverUri));
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
