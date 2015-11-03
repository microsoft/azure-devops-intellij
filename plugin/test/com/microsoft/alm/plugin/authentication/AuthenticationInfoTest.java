// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.alm.plugin.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests to verify the AuthenticationInfo interface implementations
 * VsoAuthenticationInfo and TfsAuthenticationInfo
 */
public class AuthenticationInfoTest extends AbstractTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void constructor() {
        final AuthenticationInfo info = new AuthenticationInfo();
        Assert.assertNull(info.getPassword());
        Assert.assertNull(info.getServerUri());
        Assert.assertNull(info.getUserName());
        Assert.assertNull(info.getUserNameForDisplay());

        final AuthenticationInfo info2 = new AuthenticationInfo("userName", "password", "serverurl", "fordisplay");
        Assert.assertEquals("userName", info2.getUserName());
        Assert.assertEquals("password", info2.getPassword());
        Assert.assertEquals("serverurl", info2.getServerUri());
        Assert.assertEquals("fordisplay", info2.getUserNameForDisplay());
    }

    @Test
    public void testIsAuthenticaitonInfoJSONWrappable() {
        try {
            final AuthenticationInfo authenticationInfo = new AuthenticationInfo("user", "password", "http://server:8080/tfs", "name");
            final String str = mapper.writeValueAsString(authenticationInfo);
            final AuthenticationInfo restored = mapper.readValue(str, AuthenticationInfo.class);
        } catch (JsonProcessingException e) {
            Assert.fail("Failed converting AuthenticationInfo to String or vice versa: " + e.getMessage());
        } catch (IOException e) {
            Assert.fail("Failed to read AuthenticationInfo object from String: " + e.getMessage());
        }
    }
}
