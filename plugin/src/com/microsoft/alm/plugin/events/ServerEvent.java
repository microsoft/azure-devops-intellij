// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

public enum ServerEvent {
    // These events are powers of 2 to allow them to be combined
    NONE(0),
    PULL_REQUESTS_CHANGED(1),
    WORK_ITEMS_CHANGED(2),
    BUILDS_CHANGED(4);

    private final int eventId;

    ServerEvent(final int eventId) {
        this.eventId = eventId;
    }

    public int getEventId() {
        return eventId;
    }
}
