// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import org.apache.commons.lang.StringUtils;

import java.util.List;

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
    private final List<ServerStatusType> changeTypes;
    private final String workspace;
    private final String computer;
    private final boolean isCandidate;
    private final String sourceItem;

    public PendingChange(final String serverItem, final String changeType) {
        this(serverItem, null, null, null, null, null, changeType, null, null, false, StringUtils.EMPTY);
    }

    public PendingChange(final String serverItem, final String localItem, final String version,
                         final String owner, final String date, final String lock,
                         final String changeType, final String workspace, final String computer,
                         final boolean isCandidate, final String sourceItem) {
        this.serverItem = serverItem;
        this.localItem = localItem;
        this.version = version;
        this.owner = owner;
        this.date = date;
        this.lock = lock;
        this.changeTypes = ServerStatusType.getServerStatusTypes(changeType);
        this.workspace = workspace;
        this.computer = computer;
        this.isCandidate = isCandidate;
        this.sourceItem = sourceItem;
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

    public List<ServerStatusType> getChangeTypes() {
        return changeTypes;
    }

    public String getWorkspace() {
        return workspace;
    }

    public boolean isCandidate() {
        return isCandidate;
    }

    public String getSourceItem() {
        return sourceItem;
    }
}
