// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.tfs.model.connector.TfsItemInfo;
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
    private final String lastModified;
    private final String fileType;

    public ItemInfo(
            String serverItem,
            String localItem,
            String serverVersion,
            String localVersion,
            String change,
            String type,
            String lastModified,
            String fileType) {
        this.serverItem = serverItem;
        this.localItem = localItem;
        this.serverVersion = serverVersion;
        this.localVersion = localVersion;
        this.change = change;
        this.type = type;
        this.lastModified = lastModified;
        this.fileType = fileType;
    }

    public static ItemInfo from(TfsItemInfo ii) {
        return new ItemInfo(
                ii.getServerItem(),
                ii.getLocalItem(),
                Integer.toString(ii.getServerVersion()),
                Integer.toString(ii.getLocalVersion()),
                ii.getChange(),
                ii.getType(),
                ii.getLastModified(),
                ii.getFileEncoding());
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

    public String getLastModified() {
        return lastModified;
    }

    public String getFileType() {
        return fileType;
    }

    public boolean isFolder() {
        return !StringUtils.equalsIgnoreCase(type, "file");
    }
}
