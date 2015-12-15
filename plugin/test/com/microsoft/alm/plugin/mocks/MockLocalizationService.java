// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.services.LocalizationService;

public class MockLocalizationService implements LocalizationService {
    public String getLocalizedMessage(final String key, Object... params) {
        return key;
    }
}
