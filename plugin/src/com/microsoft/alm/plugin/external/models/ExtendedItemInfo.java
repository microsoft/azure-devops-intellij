// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.tfs.model.connector.TfsExtendedItemInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Extended {@link ItemInfo} data: includes lock information.
 */
public class ExtendedItemInfo extends ItemInfo {
    @Nullable private final String lock;
    @Nullable private final String lockOwner;

    public ExtendedItemInfo(
            String serverItem,
            String localItem,
            String serverVersion,
            String localVersion,
            String change,
            String type,
            String lastModified,
            String fileType,
            @Nullable String lock,
            @Nullable String lockOwner) {
        super(serverItem, localItem, serverVersion, localVersion, change, type, lastModified, fileType);
        this.lock = lock;
        this.lockOwner = lockOwner;
    }

    public static ExtendedItemInfo from(TfsExtendedItemInfo ii) {
        return new ExtendedItemInfo(
                ii.getServerItem(),
                ii.getLocalItem(),
                Integer.toString(ii.getServerVersion()),
                Integer.toString(ii.getLocalVersion()),
                ii.getChange(),
                ii.getType(),
                ii.getLastModified(),
                ii.getFileEncoding(),
                ii.getLock(),
                ii.getLockOwner());
    }

    @Nullable
    public String getLock() {
        return lock;
    }

    @Nullable
    public String getLockOwner() {
        return lockOwner;
    }
}
