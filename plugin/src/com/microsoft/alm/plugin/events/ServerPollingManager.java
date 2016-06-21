// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import com.microsoft.alm.common.utils.ArgumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ServerPollingManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerPollingManager.class);
    private static final int DEFAULT_POLLING_INTERVAL = 5 * 60 * 1000; // TODO eventually get from settings

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
        logger.info("ServerPollingManager created");
        ArgumentHelper.checkNotNull(eventManager, "eventManager");
        this.eventManager = eventManager;
        timer = new Timer(Integer.MAX_VALUE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timerFired();
            }
        });
    }

    public void startPolling() {
        startPolling(DEFAULT_POLLING_INTERVAL);
    }

    public void startPolling(final int intervalInMilliSeconds) {
        logger.info("Polling started");
        polling = true;
        if (!timer.isRunning()) {
            timer.setInitialDelay(intervalInMilliSeconds);
            timer.start();
        }
    }

    public void stopPolling() {
        logger.info("Polling stopped");
        polling = false;
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    private void timerFired() {
        logger.info("Timer fired");
        timer.stop();
        if (!polling) {
            return;
        }

        // TODO: Ideally we would contact the server and see what actually changed, but there isn't any call for that, yet
        // Fire all changed events
        final Map<String,Object> eventContext = new HashMap<String,Object>();
        eventContext.put("sender", "pollingManager");
        eventManager.triggerAllEvents(eventContext);
        timer.restart();
    }
}
