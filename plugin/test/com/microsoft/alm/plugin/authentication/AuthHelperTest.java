// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Assert;
import org.junit.Test;

import static com.microsoft.alm.plugin.authentication.AuthenticationInfo.CredsType.NTLM;

public class AuthHelperTest {
    @Test
    public void getCredentials() {
        // Basic TFS
        final AuthenticationInfo info = new AuthenticationInfo("name1", "pass1", "server1", "display1");
        final Credentials credentials = AuthHelper.getCredentials(ServerContext.Type.TFS, info);
        Assert.assertTrue(credentials instanceof NTCredentials);
        Assert.assertEquals("name1", credentials.getUserPrincipal().getName());
        Assert.assertEquals("pass1", credentials.getPassword());

        // Basic VSO
        final AuthenticationInfo info2 = new AuthenticationInfo("name2", "pass2", "server2", "display2");
        final Credentials credentials2 = AuthHelper.getCredentials(ServerContext.Type.VSO, info2);
        Assert.assertTrue(credentials2 instanceof UsernamePasswordCredentials);
        Assert.assertEquals("name2", credentials2.getUserPrincipal().getName());
        Assert.assertEquals("pass2", credentials2.getPassword());

        // domain parsing
        createAndVerifyNTCredentials("domain", "name", "domain/name", "pass");
        createAndVerifyNTCredentials("domain", "name", "domain\\name", "pass");
        createAndVerifyNTCredentials("domain", "name", "name@domain", "pass");
    }

    private void createAndVerifyNTCredentials(final String domain, final String name, final String domainName, final String pass) {
        final AuthenticationInfo info = new AuthenticationInfo(domainName, pass, "server", "display");
        final Credentials credentials = AuthHelper.getCredentials(ServerContext.Type.TFS, info);
        Assert.assertTrue(credentials instanceof NTCredentials);
        Assert.assertEquals(name, ((NTCredentials) credentials).getUserName());
        Assert.assertEquals(domain, ((NTCredentials) credentials).getDomain().toLowerCase());
        if(SystemHelper.getComputerName() != null) { //coming back null when running from command line on Mac, works inside IDE
            Assert.assertEquals(SystemHelper.getComputerName().toLowerCase(), ((NTCredentials) credentials).getWorkstation().toLowerCase());
        }
        Assert.assertEquals(pass, credentials.getPassword());
    }

    @Test
    public void createAuthInfo() {
        final Credentials credentials = new UsernamePasswordCredentials("userName", "password");
        final AuthenticationInfo info = AuthHelper.createAuthenticationInfo("server", credentials, NTLM);
        Assert.assertEquals("userName", info.getUserName());
        Assert.assertEquals("password", info.getPassword());
        Assert.assertEquals("server", info.getServerUri());
        Assert.assertEquals("userName", info.getUserNameForDisplay());
    }
}
