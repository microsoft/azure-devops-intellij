// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.mocks.MockCredentialsPrompt;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class TfsAuthenticationProviderTest extends AbstractTest {
    @Test
    public void getAuthenticationInfo() {
        final AuthenticationInfo info = new AuthenticationInfo("userName", "", "", "");
        final ServerContext tfsContext = new ServerContextBuilder().type(ServerContext.Type.TFS)
                .uri(TfsAuthenticationProvider.TFS_LAST_USED_URL).authentication(info).build();
        ServerContextManager.getInstance().add(tfsContext);
        final TfsAuthenticationProvider provider = TfsAuthenticationProvider.getInstance();
        Assert.assertEquals(info, provider.getAuthenticationInfo(TfsAuthenticationProvider.TFS_LAST_USED_URL));
        Assert.assertTrue(provider.isAuthenticated(TfsAuthenticationProvider.TFS_LAST_USED_URL));
    }

    @Test
    public void clearAuthenticationDetails() {
        final AuthenticationInfo info = new AuthenticationInfo("userName", "", "", "");
        final ServerContext tfsContext = new ServerContextBuilder().type(ServerContext.Type.TFS)
                .uri(TfsAuthenticationProvider.TFS_LAST_USED_URL).authentication(info).build();
        ServerContextManager.getInstance().add(tfsContext);
        final TfsAuthenticationProvider provider = TfsAuthenticationProvider.getInstance();
        Assert.assertEquals(info, provider.getAuthenticationInfo(TfsAuthenticationProvider.TFS_LAST_USED_URL));
        Assert.assertTrue(provider.isAuthenticated(TfsAuthenticationProvider.TFS_LAST_USED_URL));
        provider.clearAuthenticationDetails(TfsAuthenticationProvider.TFS_LAST_USED_URL);
        Assert.assertEquals(null, provider.getAuthenticationInfo(TfsAuthenticationProvider.TFS_LAST_USED_URL));
        Assert.assertFalse(provider.isAuthenticated(TfsAuthenticationProvider.TFS_LAST_USED_URL));
    }

    @Test
    public void authenticate_succeeded() {
        final SettableFuture<Boolean> futureAuthenticatingCalled = SettableFuture.create();
        final SettableFuture<AuthenticationInfo> futureAuthenticated = SettableFuture.create();
        final AuthenticationInfo info = new AuthenticationInfo("userName0", "", "", "");
        final ServerContext tfsContext = new ServerContextBuilder().type(ServerContext.Type.TFS)
                .uri(TfsAuthenticationProvider.TFS_LAST_USED_URL).authentication(info).build();
        ServerContextManager.getInstance().add(tfsContext);
        final TfsAuthenticationProvider provider = TfsAuthenticationProvider.getInstance();
        provider.authenticateAsync("serverUrl", new AuthenticationListener() {
            @Override
            public void authenticating() {
                futureAuthenticatingCalled.set(true);
            }

            @Override
            public void authenticated(AuthenticationInfo authenticationInfo, Throwable throwable) {
                futureAuthenticated.set(authenticationInfo);
            }
        });

        try {
            // Make sure that the cred prompt was called
            AuthenticationInfo info2 = futureAuthenticated.get();
            Assert.assertEquals("userName1", info2.getUserName());

            // Make sure that authenticating was called
            Assert.assertTrue(futureAuthenticatingCalled.get());

        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (ExecutionException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void authenticate_failed() {
        final String serverUrl = "http://authenticate_failed/path";
        // register the server to fail
        ((MockCredentialsPrompt)PluginServiceProvider.getInstance().getCredentialsPrompt()).registerServer(serverUrl, false);
        final SettableFuture<Boolean> futureAuthenticatingCalled = SettableFuture.create();
        final SettableFuture<Throwable> futureAuthenticated = SettableFuture.create();
        final AuthenticationInfo info = new AuthenticationInfo("userName0", "", "", "");
        final ServerContext tfsContext = new ServerContextBuilder().type(ServerContext.Type.TFS)
                .uri(TfsAuthenticationProvider.TFS_LAST_USED_URL).authentication(info).build();
        ServerContextManager.getInstance().add(tfsContext);
        final TfsAuthenticationProvider provider = TfsAuthenticationProvider.getInstance();
        provider.authenticateAsync(serverUrl, new AuthenticationListener() {
            @Override
            public void authenticating() {
                futureAuthenticatingCalled.set(true);
            }

            @Override
            public void authenticated(final AuthenticationInfo authenticationInfo, final Throwable throwable) {
                futureAuthenticated.set(throwable);
            }
        });

        try {
            // Make sure that the cred prompt was called
            final Throwable throwable = futureAuthenticated.get();
            Assert.assertNotNull(throwable);

            // Make sure that authenticating was called
            Assert.assertTrue(futureAuthenticatingCalled.get());

        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (ExecutionException e) {
            Assert.fail(e.getMessage());
        }

        // unregister the server
        ((MockCredentialsPrompt)PluginServiceProvider.getInstance().getCredentialsPrompt()).unregisterServer(serverUrl);
    }
}
