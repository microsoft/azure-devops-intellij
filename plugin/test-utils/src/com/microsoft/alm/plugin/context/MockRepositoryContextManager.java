// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

public class MockRepositoryContextManager extends RepositoryContextManager {
    private RepositoryContext forcedContext;

    @Override
    public synchronized RepositoryContext get(String localRootFolder) {
        return forcedContext == null ? super.get(localRootFolder) : forcedContext;
    }

    public void useContext(RepositoryContext context) {
        forcedContext = context;
    }
}
