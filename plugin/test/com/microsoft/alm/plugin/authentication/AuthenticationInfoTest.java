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
    public void testIsAuthenticaitonInfoJSONWrappable() {
        try {
            AuthenticationInfo authenticationInfo = new AuthenticationInfo("user", "password", "http://server:8080/tfs", "name");
            String str = mapper.writeValueAsString(authenticationInfo);
            AuthenticationInfo restored = mapper.readValue(str, AuthenticationInfo.class);
        } catch (JsonProcessingException e) {
            Assert.fail("Failed converting AuthenticationInfo to String or vice versa: " + e.getMessage());
        } catch (IOException e) {
            Assert.fail("Failed to read AuthenticationInfo object from String: " + e.getMessage());
        }
    }
}
