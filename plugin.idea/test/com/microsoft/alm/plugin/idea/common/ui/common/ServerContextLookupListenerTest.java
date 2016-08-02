// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.google.common.collect.ImmutableList;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockServerContext;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockServerContextLookupPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockServerContextLookupOperation;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerContextLookupListenerTest extends IdeaAbstractTest {
    MockServerContextLookupPageModel pageModel = new MockServerContextLookupPageModel();
    ServerContextLookupListener listener = new ServerContextLookupListener(pageModel);

    /**
     * These tests will make sure that the constructors work as expected.
     */
    @Test
    public void testConstructor_Happy() {
        new ServerContextLookupListener(new MockServerContextLookupPageModel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_PageModelNull() {
        new ServerContextLookupListener(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadContexts_ContextListNull() {
        listener.loadContexts(null, ServerContextLookupOperation.ContextScope.PROJECT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadContexts_ContextListEmpty() {
        listener.loadContexts(Collections.EMPTY_LIST, ServerContextLookupOperation.ContextScope.PROJECT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadContexts_ScopeNull() {
        ServerContext context = new MockServerContext(ServerContext.Type.TFS, null, URI.create("http://notanurl1"), null, null, null);
        listener.loadContexts(ImmutableList.of(context), null);
    }

    @Test
    public void testLoadContexts() {
        MockServerContextLookupPageModel pageModel = new MockServerContextLookupPageModel();
        ServerContextLookupListener listener = new ServerContextLookupListener(pageModel);
        List<ServerContext> contexts = new ArrayList<ServerContext>();
        contexts.add(new MockServerContext(ServerContext.Type.TFS, null, URI.create("http://notanurl0"), null, null, null));
        assertEquals(1, contexts.size());

        // Add some existing contexts to the page model so the clear has something to do
        pageModel.contexts.add(new MockServerContext(ServerContext.Type.TFS, null, URI.create("http://notanurl1"), null, null, null));
        pageModel.contexts.add(new MockServerContext(ServerContext.Type.TFS, null, URI.create("http://notanurl2"), null, null, null));
        assertEquals(2, pageModel.contexts.size());

        MockServerContextLookupOperation operation = new MockServerContextLookupOperation(
                contexts, ServerContextLookupOperation.ContextScope.REPOSITORY);
        listener.loadContexts(operation);
        // Validate that the on started event prepares the page
        operation.onLookupStarted();
        assertTrue(pageModel.loading);
        assertEquals(0, pageModel.contexts.size());
        // Validate that the on results event updates the page (but keeps it loading)
        operation.onLookupResults(contexts);
        assertEquals(contexts.size(), pageModel.contexts.size());
        assertTrue(pageModel.loading);
        // Validate that the on completed event updates the page
        operation.onLookupCompleted();
        assertFalse(pageModel.loading);
        assertEquals(contexts.size(), pageModel.contexts.size());
    }
}
