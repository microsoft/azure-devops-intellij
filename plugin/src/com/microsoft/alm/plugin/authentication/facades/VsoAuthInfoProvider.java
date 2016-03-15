// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.auth.secret.Token;
import com.microsoft.alm.auth.secret.TokenPair;
import com.microsoft.alm.auth.secret.VsoTokenScope;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.provider.JaxrsClientProvider;
import com.microsoft.alm.storage.InsecureInMemoryStore;
import com.microsoft.alm.storage.SecretStore;
import com.microsoft.visualstudio.services.account.AccountHttpClient;
import com.microsoft.visualstudio.services.account.Profile;

import javax.ws.rs.client.Client;
import java.net.URI;

public class VsoAuthInfoProvider implements AuthenticationInfoProvider {

    private static final String CLIENT_ID = "502ea21d-e545-4c66-9129-c352ec902969";
    private static final String REDIRECT_URL = "https://xplatalm.com";

    final SecretStore<TokenPair> accessTokenStore;
    final SecretStore<Token> tokenStore;
    final VstsPatAuthenticator vstsPatAuthenticator;
    final JaxrsClientProvider jaxrsClientProvider;

    private VsoAuthInfoProvider() {
        accessTokenStore = new InsecureInMemoryStore<TokenPair>();
        tokenStore = new InsecureInMemoryStore<Token>();

        final OAuth2Authenticator oAuth2Authenticator = new OAuth2Authenticator(OAuth2Authenticator.MANAGEMENT_CORE_RESOURCE,
                CLIENT_ID, URI.create(REDIRECT_URL), accessTokenStore);
        jaxrsClientProvider = new JaxrsClientProvider(oAuth2Authenticator);

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

        try {
            final Client client = jaxrsClientProvider.getVstsGlobalClient();

            //TODO: this is a dependency on the aad-pat-generator jar which is going away.  When we update to consume
            //v0.4.3 Rest client, we can get rid of this one
            final AccountHttpClient accountHttpClient
                    = new AccountHttpClient(client, OAuth2Authenticator.APP_VSSPS_VISUALSTUDIO);

            final Profile me = accountHttpClient.getMyProfile();
            final String emailAddress = me.getCoreAttributes().getEmailAddress().getValue();

            final String tokenDescription = AuthHelper.getTokenDescription(emailAddress);

            final Token token = vstsPatAuthenticator.getVstsGlobalPat(VsoTokenScope.or(VsoTokenScope.CodeAll, VsoTokenScope.WorkRead),
                    tokenDescription, PromptBehavior.AUTO);

            final AuthenticationInfo authenticationInfo = new AuthenticationInfo(me.getId().toString(),
                    token.Value, serverUri,  emailAddress);

            authenticationInfoFuture.set(authenticationInfo);

        } catch (Throwable t) {
            authenticationInfoFuture.setException(t);
        }

        Futures.addCallback(authenticationInfoFuture, callback);
    }

}
