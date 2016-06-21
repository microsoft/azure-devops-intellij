// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ServerEventManagerTest {
    private static class EventCounter {
        private int eventCount = 0;

        public void increment() {
            synchronized (this) {
                eventCount++;
            }
        }

        public int getEventCount() {
            synchronized (this) {
                return eventCount;
            }
        }
    }

    @Test
    public void testAddRemoveListener() {
        final EventCounter counter = new EventCounter();
        final ServerEventListener listener = new ServerEventListener() {
            @Override
            public void serverChanged(final ServerEvent event, final Map<String,Object> contextMap) {
                synchronized (EventCounter.class) {
                    counter.increment();
                }
            }
        };

        final ServerEventManager manager = new ServerEventManager();
        // Verify that no listeners are added yet
        Assert.assertEquals(0, manager.getListenerCount());
        // Make sure that add actually adds a listener
        manager.addListener(listener);
        Assert.assertEquals(1, manager.getListenerCount());
        // Make sure that calling add again won't actually add it again
        manager.addListener(listener);
        Assert.assertEquals(1, manager.getListenerCount());
        // Make sure remove actually removes it
        manager.removeListener(listener);
        Assert.assertEquals(0, manager.getListenerCount());
        // Make sure that calling remove again doesn't cause any problems
        manager.removeListener(listener);
        Assert.assertEquals(0, manager.getListenerCount());

        // make sure that we didn't cause the event to be fired in any of the above calls
        Assert.assertEquals(0, counter.getEventCount());
    }

    @Test
    public void testTriggerEvent() {
        final EventCounter counterBuild = new EventCounter();
        final EventCounter counterWit = new EventCounter();
        final EventCounter counterPullRequest = new EventCounter();
        final ServerEventListener listener = new ServerEventListener() {
            @Override
            public void serverChanged(final ServerEvent event, final Map<String,Object> contextMap) {
                if (event == ServerEvent.BUILDS_CHANGED) {
                    counterBuild.increment();
                }
                if (event == ServerEvent.WORK_ITEMS_CHANGED) {
                    counterWit.increment();
                }
                if (event == ServerEvent.PULL_REQUESTS_CHANGED) {
                    counterPullRequest.increment();
                }
            }
        };

        final ServerEventManager manager = new ServerEventManager();
        // Verify that no listeners are added yet
        Assert.assertEquals(0, manager.getListenerCount());
        // Make sure that add actually adds a listener
        manager.addListener(listener);
        Assert.assertEquals(1, manager.getListenerCount());

        try {
            manager.triggerEvent(null, null);
            Assert.fail("null was accepted in the triggerEvent method");
        } catch (IllegalArgumentException e) {
            // This is expected
        }

        // Make sure that no events have been fired yet
        Assert.assertEquals(0, counterBuild.getEventCount());
        Assert.assertEquals(0, counterPullRequest.getEventCount());
        Assert.assertEquals(0, counterWit.getEventCount());

        // Make sure that firing an event only triggers the event passed in
        manager.triggerEvent(ServerEvent.BUILDS_CHANGED, null);
        Assert.assertEquals(1, counterBuild.getEventCount());
        Assert.assertEquals(0, counterPullRequest.getEventCount());
        Assert.assertEquals(0, counterWit.getEventCount());

        manager.triggerEvent(ServerEvent.PULL_REQUESTS_CHANGED, null);
        Assert.assertEquals(1, counterBuild.getEventCount());
        Assert.assertEquals(1, counterPullRequest.getEventCount());
        Assert.assertEquals(0, counterWit.getEventCount());

        manager.triggerEvent(ServerEvent.WORK_ITEMS_CHANGED, null);
        Assert.assertEquals(1, counterBuild.getEventCount());
        Assert.assertEquals(1, counterPullRequest.getEventCount());
        Assert.assertEquals(1, counterWit.getEventCount());

        // Make sure that multiple events fire individually and don't affect other events
        manager.triggerEvent(ServerEvent.BUILDS_CHANGED, null);
        manager.triggerEvent(ServerEvent.BUILDS_CHANGED, null);
        manager.triggerEvent(ServerEvent.BUILDS_CHANGED, null);
        Assert.assertEquals(4, counterBuild.getEventCount());
        Assert.assertEquals(1, counterPullRequest.getEventCount());
        Assert.assertEquals(1, counterWit.getEventCount());
    }
}