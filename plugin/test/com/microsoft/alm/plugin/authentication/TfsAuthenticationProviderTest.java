// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.mocks.MockCredentialsPrompt;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import jersey.repackaged.com.google.common.util.concurrent.SettableFuture;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class TfsAuthenticationProviderTest extends AbstractTest {
    @Test
    public void constructor() {
        // Make sure basic ctor works
        final TfsAuthenticationProvider provider = new TfsAuthenticationProvider();

        // Make sure that that null is not allowed
        try {
            final TfsAuthenticationProvider provider2 = new TfsAuthenticationProvider(null);
            Assert.fail("constructor allows null info");
        } catch (AssertionError error) {
            // correct error thrown
        }
    }

    @Test
    public void getAuthenticationInfo() {
        final AuthenticationInfo info = new AuthenticationInfo("userName", "", "", "");
        final TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
        Assert.assertEquals(info, provider.getAuthenticationInfo());
        Assert.assertTrue(provider.isAuthenticated());
    }

    @Test
    public void clearAuthenticationDetails() {
        final AuthenticationInfo info = new AuthenticationInfo("userName", "", "", "");
        final TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
        Assert.assertEquals(info, provider.getAuthenticationInfo());
        Assert.assertTrue(provider.isAuthenticated());
        provider.clearAuthenticationDetails();
        Assert.assertEquals(null, provider.getAuthenticationInfo());
        Assert.assertFalse(provider.isAuthenticated());
    }

    @Test
    public void authenticate_succeeded() {
        final SettableFuture<Boolean> futureAuthenticatingCalled = SettableFuture.create();
        final SettableFuture<AuthenticationInfo> futureAuthenticated = SettableFuture.create();
        final AuthenticationInfo info = new AuthenticationInfo("userName0", "", "", "");
        final TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
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
        final TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
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
