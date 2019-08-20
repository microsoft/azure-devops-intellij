// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.providers;

import com.microsoft.alm.auth.oauth.OAuth2UseragentValidator;
import com.microsoft.alm.oauth2.useragent.ProviderScanner;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link OAuth2UseragentValidator} implementation that allows to override a provider scanner.
 */
public class ConfigurableOAuth2UserAgentValidator extends OAuth2UseragentValidator {
    @NotNull private final ProviderScanner scanner;

    public ConfigurableOAuth2UserAgentValidator(@NotNull ProviderScanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public boolean isOAuth2ProviderAvailable() {
        return scanner.findCompatibleProvider() != null;
    }
}
