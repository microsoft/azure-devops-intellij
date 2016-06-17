// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import com.microsoft.alm.common.utils.ArgumentHelper;

import java.util.ArrayList;
import java.util.List;

public class ServerEventManager {
    private final List<ServerEventListener> listeners = new ArrayList<ServerEventListener>(5);

    private static class Holder {
        private static final ServerEventManager INSTANCE = new ServerEventManager();
    }

    public static ServerEventManager getInstance() {
        return Holder.INSTANCE;
    }

    protected ServerEventManager() {
    }

    protected int getListenerCount() {
        return listeners.size();
    }

    public void triggerEvent(final ServerEvent event) {
        ArgumentHelper.checkNotNull(event, "event");
        for (final ServerEventListener listener : listeners) {
            listener.serverChanged(event);
        }
    }

    public void addListener(final ServerEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(final ServerEventListener listener) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i) == listener) {
                listeners.remove(i);
                break;
            }
        }
    }
}
