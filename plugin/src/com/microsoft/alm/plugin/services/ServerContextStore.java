// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.alm.plugin.context.ServerContext;

/**
 * This interface represents the service that allows us to load/save/forget server context information.
 */
public interface ServerContextStore {
    String DEFAULT_CONTEXT = "";
    void forgetServerContext(String key);
    String getKey(ServerContext context);
    ServerContext loadServerContext(String key);
    void saveServerContext(String key, ServerContext context);
}
