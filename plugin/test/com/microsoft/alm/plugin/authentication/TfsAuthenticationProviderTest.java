// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.AbstractTest;
import jersey.repackaged.com.google.common.util.concurrent.SettableFuture;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class TfsAuthenticationProviderTest extends AbstractTest {
    @Test
    public void constructor() {
        // Make sure basic ctor works
        TfsAuthenticationProvider provider = new TfsAuthenticationProvider();

        // Make sure that that null is not allowed
        try {
            TfsAuthenticationProvider provider2 = new TfsAuthenticationProvider(null);
            Assert.fail("constructor allows null info");
        } catch (AssertionError error) {
            // correct error thrown
        }
    }

    @Test
    public void getAuthenticationInfo() {
        AuthenticationInfo info = new AuthenticationInfo("userName", "", "", "");
        TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
        Assert.assertEquals(info, provider.getAuthenticationInfo());
        Assert.assertTrue(provider.isAuthenticated());
    }

    @Test
    public void clearAuthenticationDetails() {
        AuthenticationInfo info = new AuthenticationInfo("userName", "", "", "");
        TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
        Assert.assertEquals(info, provider.getAuthenticationInfo());
        Assert.assertTrue(provider.isAuthenticated());
        provider.clearAuthenticationDetails();
        Assert.assertEquals(null, provider.getAuthenticationInfo());
        Assert.assertFalse(provider.isAuthenticated());
    }

    @Test
    public void authenticate() {
        final SettableFuture<Boolean> futureAuthenticatingCalled = SettableFuture.create();
        final SettableFuture<AuthenticationInfo> futureAuthenticated = SettableFuture.create();
        AuthenticationInfo info = new AuthenticationInfo("userName0", "", "", "");
        TfsAuthenticationProvider provider = new TfsAuthenticationProvider(info);
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

}
