// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.alm.plugin.context.ServerContext;

import java.util.List;

/**
 * This interface represents the service that allows us to load/save/forget server context information.
 */
public interface ServerContextStore {

    void forgetServerContext(final String key);

    List<ServerContext> restoreServerContexts();

    void saveServerContext(final ServerContext context);
}
