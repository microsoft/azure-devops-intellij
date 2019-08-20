// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.providers;

import com.microsoft.alm.auth.oauth.AzureAuthority;
import com.microsoft.alm.auth.oauth.helper.AzureAuthorityProvider;
import com.microsoft.alm.oauth2.useragent.UserAgent;
import org.jetbrains.annotations.NotNull;

/**
 * A class derived from {@link AzureAuthorityProvider} that allows to override the {@link UserAgent} passed to the
 * {@link AzureAuthority} created.
 */
public class ConfigurableAzureAuthorityProvider extends AzureAuthorityProvider {
    @NotNull
    private final UserAgent userAgent;

    public ConfigurableAzureAuthorityProvider(@NotNull UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    protected AzureAuthority getDefaultAzureAuthority() {
        return new AzureAuthority(AzureAuthority.DefaultAuthorityHostUrl, userAgent);
    }

    @Override
    protected AzureAuthority getAzureAuthorityForHostUrl(String hostUrl) {
        return new AzureAuthority(hostUrl, userAgent);
    }
}
