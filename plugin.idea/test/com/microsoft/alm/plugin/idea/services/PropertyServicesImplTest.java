// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class PropertyServicesImplTest extends IdeaAbstractTest {
    @Test
    public void testSetProperty() {
        PropertyServiceImpl service = new PropertyServiceImpl();
        service.setProperty("property1Key", "property1Value");
        Assert.assertEquals("property1Value", service.getProperties().get("property1Key"));

        // set it to null to remove it
        service.setProperty("property1Key", null);
        Assert.assertEquals(null, service.getProperties().get("property1Key"));
    }

    @Test
    public void testGetProperty() {
        PropertyServiceImpl service = new PropertyServiceImpl();
        service.setProperty("property1Key", "property1Value");
        Assert.assertEquals("property1Value", service.getProperty("property1Key"));

        // set it to null to remove it
        service.setProperty("property1Key", null);
        Assert.assertEquals(null, service.getProperty("property1Key"));
    }

}
