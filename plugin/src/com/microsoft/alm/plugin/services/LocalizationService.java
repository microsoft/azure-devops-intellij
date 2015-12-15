// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

public interface LocalizationService {

    String getLocalizedMessage(final String key, Object... params);

}
