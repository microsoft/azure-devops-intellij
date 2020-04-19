// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.intellij.openapi.components.ServiceManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class used to manage RepositoryContext objects.
 * This is just an in memory cache for now since creating the RepositoryContext instances for TFVC is so expensive
 */
public class RepositoryContextManager {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryContextManager.class);

    private Map<String, RepositoryContext> contextMap = new HashMap<String, RepositoryContext>();

    public static RepositoryContextManager getInstance() {
        return ServiceManager.getService(RepositoryContextManager.class);
    }

    public synchronized void add(final RepositoryContext context) {
        if (context != null) {
            final String key = context.getLocalRootFolder();
            contextMap.put(key, context);
        }
    }

    public synchronized RepositoryContext get(final String localRootFolder) {
        if (!StringUtils.isEmpty(localRootFolder)) {
            final RepositoryContext context = contextMap.get(localRootFolder);
            return context;
        }

        return null;
    }

    public synchronized void remove(final String localRootFolder) {
        if (StringUtils.isEmpty(localRootFolder)) {
            return;
        }
        contextMap.remove(localRootFolder);
    }

}
