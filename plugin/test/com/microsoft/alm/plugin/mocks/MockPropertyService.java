// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.services.PropertyService;

import java.util.HashMap;
import java.util.Map;

public class MockPropertyService implements PropertyService {
    private Map<String, String> map = new HashMap<String, String>();

    @Override
    public String getProperty(String propertyName) {
        return map.get(propertyName);
    }

    @Override
    public void setProperty(String propertyName, String value) {
        map.put(propertyName, value);
    }
}
