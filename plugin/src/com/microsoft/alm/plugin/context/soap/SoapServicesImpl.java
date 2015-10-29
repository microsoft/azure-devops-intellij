// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.soap;

import com.microsoft.alm.plugin.context.ServerContext;

public class SoapServicesImpl implements SoapServices {

    private final ServerContext context;
    private CatalogService catalogService;

    public SoapServicesImpl(final ServerContext context) {
        this.context = context;
    }

    public synchronized CatalogService getCatalogService() {
        if (catalogService == null) {
            catalogService = new CatalogServiceImpl(context);
        }
        return catalogService;
    }

}
