// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryContextManagerTest extends AbstractTest {
    @Test
    public void testSingletonConstructor() {
        Assert.assertNotNull(RepositoryContextManager.getInstance());
    }

    @Test
    public void testAdd() {
        final String localRootFolder = "/path/path/";
        final RepositoryContextManager manager = new RepositoryContextManager();
        final RepositoryContext context = RepositoryContext.createGitContext(localRootFolder, "repo1", "branch1", "url1");
        Assert.assertNull(manager.get(localRootFolder));
        manager.add(context);
        Assert.assertEquals(context, manager.get(localRootFolder));
    }

    @Test
    public void testAdd_empty() {
        final RepositoryContextManager manager = new RepositoryContextManager();
        manager.add(null);
    }

    @Test
    public void testGet_empty() {
        final RepositoryContextManager manager = new RepositoryContextManager();
        Assert.assertNull(manager.get(null));
    }

    @Test
    public void testRemove() {
        final String localRootFolder = "/path/path/";
        final RepositoryContextManager manager = new RepositoryContextManager();
        final RepositoryContext context = RepositoryContext.createGitContext(localRootFolder, "repo1", "branch1", "url1");
        // Make sure remove doesn't throw if its not there
        manager.remove(localRootFolder);

        // Add it and make sure its there
        manager.add(context);
        Assert.assertEquals(context, manager.get(localRootFolder));

        // Make sure remove removed it
        manager.remove(localRootFolder);
        Assert.assertNull(manager.get(localRootFolder));
    }

    @Test
    public void testRemove_empty() {
        final RepositoryContextManager manager = new RepositoryContextManager();
        manager.remove(null);
    }

}
