// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.common.utils.SystemHelper;
import org.apache.commons.lang.StringUtils;

/**
 * This class represents the data returned by the TF command 'info'.
 */
public class ItemInfo {
    private final String serverItem;
    private final String localItem;
    private final String localVersion;
    private final String serverVersion;
    private final String change;
    private final String type;
    private final String lock;
    private final String lockOwner;
    private final String deletionId;
    private final String lastModified;
    private final String fileType;
    private final String fileSize;

    public ItemInfo(final String serverItem, final String localItem, final String serverVersion, final String localVersion,
                    final String change, final String type, final String lock, final String lockOwner, final String deletionId,
                    final String lastModified, final String fileType, final String fileSize) {
        this.serverItem = serverItem;
        this.localItem = localItem;
        this.serverVersion = serverVersion;
        this.localVersion = localVersion;
        this.change = change;
        this.type = type;
        this.lock = lock;
        this.lockOwner = lockOwner;
        this.deletionId = deletionId;
        this.lastModified = lastModified;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    public String getServerItem() {
        return serverItem;
    }

    public String getLocalItem() {
        return localItem;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public int getServerVersionAsInt() {
        return SystemHelper.toInt(serverVersion, 0);
    }

    public String getLocalVersion() {
        return localVersion;
    }

    public int getLocalVersionAsInt() {
        return SystemHelper.toInt(localVersion, 0);
    }

    public String getChange() {
        return change;
    }

    public String getType() {
        return type;
    }

    public String getLock() {
        return lock;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public String getDeletionId() {
        return deletionId;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getFileType() {
        return fileType;
    }

    public boolean isFolder() {
        return !StringUtils.equalsIgnoreCase(fileType, "file");
    }

    public String getFileSize() {
        return fileSize;
    }
}
