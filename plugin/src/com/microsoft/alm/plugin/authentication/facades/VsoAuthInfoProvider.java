// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.oauth2.useragent.AuthorizationException;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
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

import javax.ws.rs.client.Client;
import java.util.concurrent.Executors;

public class VsoAuthInfoProvider implements AuthenticationInfoProvider {

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

        // start the authentication on yet another thread
        final Runnable authenticateTask = new Runnable() {
            @Override
            public void run() {

                // Callback for the Device Flow dialog to cancel the current authenticating process.
                // Normally this is hooked up to the cancel button so if user cancels, we do not wait forever in
                // a polling loop.  This doesn't stop the polling currently, there is no clean way to shutdown a thread.
                // We just let the GUI continue, and forget the polling thread.  It eventually will stop after certain
                // period of time
                // TODO: add an explicit time limit for the polling to stop in the auth library, right now it depends
                // on how long the server think it is reasonable
                final Action<String> cancellationCallback = new Action<String>() {
                    @Override
                    public void call(final String reasonForCancel) {
                        authenticationInfoFuture.setException(new AuthorizationException(reasonForCancel));
                    }
                };

                // Must share the same accessTokenStore with the member variable vstsPatAuthenticator to avoid prompt the user
                // when we generate PAT
                final OAuth2Authenticator oAuth2Authenticator = OAuth2Authenticator.getAuthenticator(
                        CLIENT_ID, REDIRECT_URL, accessTokenStore, deviceFlowResponsePrompt.getCallback(cancellationCallback));

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
                                = new AccountHttpClient(client, OAuth2Authenticator.APP_VSSPS_VISUALSTUDIO);

                        final Profile me = accountHttpClient.getMyProfile();
                        final String emailAddress = me.getCoreAttributes().getEmailAddress().getValue();

                        final String tokenDescription = AuthHelper.getTokenDescription(emailAddress);

                        final Token token = vstsPatAuthenticator.getPersonalAccessToken(
                                VsoTokenScope.or(VsoTokenScope.CodeAll, VsoTokenScope.WorkRead),
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
            }
        };
        Futures.addCallback(authenticationInfoFuture, callback);

        // start the authentication job
        executorService.submit(authenticateTask);
    }

    @Override
    public void clearAuthenticationInfo(final String serverUri) {
        // Only generates global PAT currently, so ignore serverUri and clear global PAT only
        this.vstsPatAuthenticator.signOut();
    }
}
