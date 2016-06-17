// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import com.microsoft.alm.common.utils.ArgumentHelper;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerPollingManager {
    private final ServerEventManager eventManager;
    private final Timer timer;
    private boolean polling = false;

    private static class Holder {
        private static final ServerPollingManager INSTANCE = new ServerPollingManager(ServerEventManager.getInstance());
    }

    public static ServerPollingManager getInstance() {
        return Holder.INSTANCE;
    }

    protected ServerPollingManager(final ServerEventManager eventManager) {
        ArgumentHelper.checkNotNull(eventManager, "eventManager");
        this.eventManager = eventManager;
        timer = new Timer(Integer.MAX_VALUE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timerFired();
            }
        });
    }

    public void startPolling(final int intervalInMilliSeconds) {
        polling = true;
        if (!timer.isRunning()) {
            timer.setInitialDelay(intervalInMilliSeconds);
            timer.start();
        }
    }

    public void stopPolling() {
        polling = false;
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    private void timerFired() {
        timer.stop();
        if (!polling) {
            return;
        }

        // TODO: Ideally we would contact the server and see what actually changed, but there isn't any call for that, yet
        // Fire each changed event
        eventManager.triggerEvent(ServerEvent.BUILDS_CHANGED);
        eventManager.triggerEvent(ServerEvent.PULL_REQUESTS_CHANGED);
        eventManager.triggerEvent(ServerEvent.WORK_ITEMS_CHANGED);
        timer.restart();
    }
}
