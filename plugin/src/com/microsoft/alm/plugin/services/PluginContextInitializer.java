// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;

public interface PluginContextInitializer extends ContextInitializer {
    String getUserAgent(final String defaultUserAgent);
}
