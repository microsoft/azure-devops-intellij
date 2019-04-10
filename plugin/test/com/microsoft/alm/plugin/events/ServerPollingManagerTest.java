// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServerPollingManagerTest {
    @Test
    public void testConstructor() {
        // Make sure we can construct one with our own event manager
        final ServerEventManager eventManager = new ServerEventManager();
        final ServerPollingManager manager = new ServerPollingManager(eventManager);

        // Make sure that nulls cause and exceptions
        try {
            final ServerPollingManager manager2 = new ServerPollingManager(null);
            Assert.fail("null should not be allowed in the constructor");
        } catch (IllegalArgumentException e) {
            // This is expected
        }
    }

    @Test
    public void testPolling() throws InterruptedException, ExecutionException, TimeoutException {
        final ServerEventManager eventManager = new ServerEventManager();
        final ServerPollingManager manager = new ServerPollingManager(eventManager);

        final SettableFuture<Boolean> buildChangedCalled = SettableFuture.create();
        final SettableFuture<Boolean> witChangedCalled = SettableFuture.create();
        final SettableFuture<Boolean> prChangedCalled = SettableFuture.create();
        eventManager.addListener(new ServerEventListener() {
            @Override
            public void serverChanged(final ServerEvent event, final Map<String,Object> contextMap) {
                if (event == ServerEvent.BUILDS_CHANGED) {
                    buildChangedCalled.set(true);
                }
                if (event == ServerEvent.PULL_REQUESTS_CHANGED) {
                    prChangedCalled.set(true);
                }
                if (event == ServerEvent.WORK_ITEMS_CHANGED) {
                    witChangedCalled.set(true);
                }
            }
        });

        // Make sure the events haven't fired yet
        Assert.assertEquals(false, buildChangedCalled.isDone());
        Assert.assertEquals(false, prChangedCalled.isDone());
        Assert.assertEquals(false, witChangedCalled.isDone());

        // Start polling every 10 ms
        manager.startPolling(10);
        // Make sure all futures get set
        Assert.assertEquals(true, buildChangedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertEquals(true, prChangedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertEquals(true, witChangedCalled.get(1, TimeUnit.SECONDS));
        manager.stopPolling();
    }
}
