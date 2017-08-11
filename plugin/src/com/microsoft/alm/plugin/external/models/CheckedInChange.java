// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.common.utils.SystemHelper;

import java.util.List;

public class CheckedInChange {
    private final String serverItem;
    private final List<ServerStatusType> changeTypes;
    private final String changeSetId;
    private final String date;

    public CheckedInChange(final String serverItem, final String changeType, final String changeSetId, final String date) {
        this.serverItem = serverItem;
        this.changeTypes = ServerStatusType.getServerStatusTypes(changeType);
        this.changeSetId = changeSetId;
        this.date = date;
    }

    public String getServerItem() {
        return serverItem;
    }

    public List<ServerStatusType> getChangeTypes() {
        return changeTypes;
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public int getChangeSetIdAsInt() {
        return SystemHelper.toInt(changeSetId, 0);
    }

    public String getDate() {
        return date;
    }
}