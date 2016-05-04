// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.provider.JaxrsClientProvider;
import com.microsoft.alm.secret.Token;
import com.microsoft.alm.secret.TokenPair;
import com.microsoft.alm.secret.VsoTokenScope;
import com.microsoft.alm.storage.InsecureInMemoryStore;
import com.microsoft.alm.storage.SecretStore;
import com.microsoft.visualstudio.services.account.webapi.AccountHttpClient;
import com.microsoft.visualstudio.services.account.webapi.model.Profile;

import javax.ws.rs.client.Client;

public class VsoAuthInfoProvider implements AuthenticationInfoProvider {

    private static final String CLIENT_ID = "97877f11-0fc6-4aee-b1ff-febb0519dd00";
    private static final String REDIRECT_URL = "https://java.visualstudio.com";

    final SecretStore<TokenPair> accessTokenStore;
    final SecretStore<Token> tokenStore;
    final VstsPatAuthenticator vstsPatAuthenticator;
    final JaxrsClientProvider jaxrsClientProvider;

    private VsoAuthInfoProvider() {
        accessTokenStore = new InsecureInMemoryStore<TokenPair>();
        tokenStore = new InsecureInMemoryStore<Token>();

        final OAuth2Authenticator oAuth2Authenticator = OAuth2Authenticator.getAuthenticator(
                CLIENT_ID, REDIRECT_URL, accessTokenStore);
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
            final Client client = jaxrsClientProvider.getClient();

            //TODO: this is a dependency on the aad-pat-generator jar which is going away.  When we update to consume
            //v0.4.3 Rest client, we can get rid of this one

            //Or we could reconsider the name of the token.  Now we call Profile endpoint just to get the email address
            //which is used in token description, but do we need it?  User can only view PATs after they login, and
            //at that time user knows which account/email they are logged in under already.  So the email provides
            //no additional value.
            final AccountHttpClient accountHttpClient
                    = new AccountHttpClient(client, OAuth2Authenticator.APP_VSSPS_VISUALSTUDIO);

            final Profile me = accountHttpClient.getMyProfile();
            final String emailAddress = me.getCoreAttributes().getEmailAddress().getValue();

            final String tokenDescription = AuthHelper.getTokenDescription(emailAddress);

            final Token token = vstsPatAuthenticator.getPersonalAccessToken(
                    VsoTokenScope.or(VsoTokenScope.CodeAll, VsoTokenScope.WorkRead),
                    tokenDescription,
                    PromptBehavior.AUTO);

            final AuthenticationInfo authenticationInfo = new AuthenticationInfo(me.getId().toString(),
                    token.Value, serverUri, emailAddress);

            authenticationInfoFuture.set(authenticationInfo);

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

}
