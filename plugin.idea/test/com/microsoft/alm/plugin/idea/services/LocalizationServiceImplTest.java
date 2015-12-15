// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class LocalizationServiceImplTest extends IdeaAbstractTest {

    @Test
    public void testGetLocalizedServerExceptionMessage() {
        LocalizationServiceImpl locService = new LocalizationServiceImpl();
        final String message = locService.getServerExceptionMessage("UnknownKey");
        Assert.assertEquals(message, "UnknownKey");

        final String message1 = locService.getServerExceptionMessage("KEY_VSO_AUTH_SESSION_EXPIRED");
        Assert.assertNotEquals(message1, "KEY_VSO_AUTH_SESSION_EXPIRED");
        Assert.assertEquals(message1, "Your previous Team Services session has expired, please 'Sign in...' again.");
    }
}
