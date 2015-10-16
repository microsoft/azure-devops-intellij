// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.common.mocks.MockServerContext;
import com.microsoft.alm.plugin.idea.ui.common.mocks.MockServerContextLookupOperation;
import com.microsoft.alm.plugin.idea.ui.common.mocks.MockServerContextLookupPageModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServerContextLookupListenerTest extends IdeaAbstractTest {
    /**
     * This test will make sure that the constructors work as expected.
     */
    @Test
    public void testConstructor() {
        try {
            ServerContextLookupListener listener = new ServerContextLookupListener(null);
            fail("constructor allowed null argument");
        } catch (AssertionError error) {
            // This is the expected result
        }

        MockServerContextLookupPageModel pageModel = new MockServerContextLookupPageModel();
        ServerContextLookupListener listener = new ServerContextLookupListener(pageModel);
    }

    @Test
    public void testLoadContexts_invalidParameters() {
        MockServerContextLookupPageModel pageModel = new MockServerContextLookupPageModel();
        ServerContextLookupListener listener = new ServerContextLookupListener(pageModel);

        // Make sure null list fails
        try {
            listener.loadContexts(null, null);
            fail("null list of contexts worked");
        } catch (AssertionError error) {
            // This is the expected result
        }

        // Make sure empty list fails
        List<ServerContext> contexts = new ArrayList<ServerContext>();
        try {
            listener.loadContexts(contexts, null);
            fail("empty list worked");
        } catch (AssertionError error) {
            // This is the expected result
        }

        // Make sure null scope fails
        contexts.add(new MockServerContext(null, URI.create("http://notanurl")));
        try {
            listener.loadContexts(contexts, null);
            fail("null scope worked");
        } catch (AssertionError error) {
            // This is the expected result
        }
    }

    @Test
    public void testLoadContexts() {
        MockServerContextLookupPageModel pageModel = new MockServerContextLookupPageModel();
        ServerContextLookupListener listener = new ServerContextLookupListener(pageModel);
        List<ServerContext> contexts = new ArrayList<ServerContext>();
        contexts.add(new MockServerContext(null, URI.create("http://notanurl0")));
        assertEquals(1, contexts.size());

        // Add some existing contexts to the page model so the clear has something to do
        pageModel.contexts.add(new MockServerContext(null, URI.create("http://notanurl1")));
        pageModel.contexts.add(new MockServerContext(null, URI.create("http://notanurl2")));
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
