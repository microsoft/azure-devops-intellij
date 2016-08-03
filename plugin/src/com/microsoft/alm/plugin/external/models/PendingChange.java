// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

/**
 * This class represents the data returned by the TF command line about a pending change.
 */
public class PendingChange {
    private final String serverItem;
    private final String localItem;
    private final String version;
    private final String owner;
    private final String date;
    private final String lock;
    private final String changeType;
    private final String workspace;
    private final String computer;

    public PendingChange(final String serverItem, final String changeType) {
        this(serverItem, null, null, null, null, null, changeType, null, null);
    }

    public PendingChange(final String serverItem, final String localItem, final String version,
                         final String owner, final String date, final String lock,
                         final String changeType, final String workspace, final String computer) {
        this.serverItem = serverItem;
        this.localItem = localItem;
        this.version = version;
        this.owner = owner;
        this.date = date;
        this.lock = lock;
        this.changeType = changeType;
        this.workspace = workspace;
        this.computer = computer;
    }

    public String getComputer() {
        return computer;
    }

    public String getServerItem() {
        return serverItem;
    }

    public String getLocalItem() {
        return localItem;
    }

    public String getVersion() {
        return version;
    }

    public String getOwner() {
        return owner;
    }

    public String getDate() {
        return date;
    }

    public String getLock() {
        return lock;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getWorkspace() {
        return workspace;
    }
}
