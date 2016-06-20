// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import com.microsoft.alm.common.utils.ArgumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class allows components to trigger events that let listening components know that something has changed
 * on the server. (Or that they should requery the server because something might have changed)
 *
 *  THREAD-SAFE
 */
public class ServerEventManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerEventManager.class);
    private final List<ServerEventListener> listeners = new ArrayList<ServerEventListener>(5);

    private static class Holder {
        private static final ServerEventManager INSTANCE = new ServerEventManager();
    }

    public static ServerEventManager getInstance() {
        return Holder.INSTANCE;
    }

    protected ServerEventManager() {
        logger.info("ServerEventManager created");
    }

    protected int getListenerCount() {
        return listeners.size();
    }

    public void triggerAllEvents(final Map<String,Object> contextMap) {
        triggerEvent(ServerEvent.BUILDS_CHANGED, contextMap);
        triggerEvent(ServerEvent.PULL_REQUESTS_CHANGED, contextMap);
        triggerEvent(ServerEvent.WORK_ITEMS_CHANGED, contextMap);
    }

    public void triggerEvent(final ServerEvent event, final Map<String,Object> contextMap) {
        ArgumentHelper.checkNotNull(event, "event");
        logger.info("triggering event: " + event.name());
        final ServerEventListener[] localListeners;
        synchronized (this.listeners) {
            // Copy the list of listeners in case someone tries to add or remove a listener while we are looping
            localListeners = this.listeners.toArray(new ServerEventListener[this.listeners.size()]);
        }
        for (int i = localListeners.length - 1; i >= 0; i--) {
            // Copy the map so no one can change it on us
            final Map<String,Object> localContext = contextMap != null ? new HashMap<String, Object>(contextMap) : new HashMap<String, Object>();
            localListeners[i].serverChanged(event, localContext);
        }
    }

    public void addListener(final ServerEventListener listener) {
        logger.info("listener added");
        synchronized (this.listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(final ServerEventListener listener) {
        logger.info("remove listener called");
        synchronized (this.listeners) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                if (listeners.get(i) == listener) {
                    listeners.remove(i);
                    break;
                }
            }
        }
    }
}
