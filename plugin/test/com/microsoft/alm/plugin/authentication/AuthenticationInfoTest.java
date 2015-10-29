// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import com.microsoftopentechnologies.auth.UserInfo;
import org.apache.http.auth.NTCredentials;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Tests to verify the AuthenticationInfo interface implementations
 * VsoAuthenticationInfo and TfsAuthenticationInfo
 */
public class AuthenticationInfoTest extends AbstractTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testIsVsoAuthenticationInfoJSONWrappable() {
        //setup objects
        final UserInfo userInfo = new UserInfo("id", "firstname", "lastname", "live", "upn", "uniquename", "tenetid");
        final AuthenticationResult r = new AuthenticationResult("tokentype", "token", "refreshtoken", 0, "resource", userInfo);
        final SessionToken s = new SessionToken();
        s.setAccessId(new UUID(1, 2));
        s.setAlternateToken("alttoken");
        s.setAuthorizationId(new UUID(2, 3));
        s.setClientId(new UUID(3, 4));
        s.setDisplayName("token name");
        s.setIsValid(true);
        s.setScope("test scope");
        s.setToken("token");
        s.setUserId(new UUID(4, 5));

        try {
            VsoAuthenticationInfo info = new VsoAuthenticationInfo("http://abc.vs.com", r, s);
            String infoStr = mapper.writeValueAsString(info);
            VsoAuthenticationInfo info1 = mapper.readValue(infoStr, VsoAuthenticationInfo.class);
        } catch(JsonProcessingException e) {
            Assert.fail("Failed converting VsoAuthenticationInfo to String or vice versa: " + e.getMessage());
        } catch(IOException e) {
            Assert.fail("Failed to read VsoAuthenticationInfo object from String: " + e.getMessage());
        }
    }

    @Test
    public void testIsTfsAuthenticaitonInfoJSONWrappable() {
        try {
            TfsAuthenticationInfo tfs = new TfsAuthenticationInfo("http://server:8080/tfs",
                    new NTCredentials("user:password"));
            String tfsStr = mapper.writeValueAsString(tfs);
            TfsAuthenticationInfo tfs1 = mapper.readValue(tfsStr, TfsAuthenticationInfo.class);
        } catch(JsonProcessingException e) {
            Assert.fail("Failed converting TfsAuthenticationInfo to String or vice versa: " + e.getMessage());
        } catch(IOException e) {
            Assert.fail("Failed to read TfsAuthenticationInfo object from String: " + e.getMessage());
        }
    }
}
