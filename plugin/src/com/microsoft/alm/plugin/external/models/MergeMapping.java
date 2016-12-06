// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a mapping between two server items that is returned by the Merge command.
 */
public class MergeMapping {
    private final String fromServerItem;
    private final String toServerItem;
    private final VersionSpec.Range fromServerItemVersion;
    private final VersionSpec toServerItemVersion;
    private final List<ServerStatusType> changeTypes;
    private final boolean isConflict;

    public MergeMapping(final String fromServerItem, final String toServerItem,
                        final VersionSpec.Range fromServerItemVersion, final VersionSpec toServerItemVersion,
                        final List<ServerStatusType> changeTypes, final boolean isConflict) {
        this.fromServerItem = fromServerItem;
        this.toServerItem = toServerItem;
        this.fromServerItemVersion = fromServerItemVersion;
        this.toServerItemVersion = toServerItemVersion;
        this.changeTypes = Collections.unmodifiableList(changeTypes);
        this.isConflict = isConflict;
    }

    public String getFromServerItem() {
        return fromServerItem;
    }

    public String getToServerItem() {
        return toServerItem;
    }

    public VersionSpec.Range getFromServerItemVersion() {
        return fromServerItemVersion;
    }

    public VersionSpec getToServerItemVersion() {
        return toServerItemVersion;
    }

    public List<ServerStatusType> getChangeTypes() {
        return changeTypes;
    }

    public boolean isConflict() {
        return isConflict;
    }
}
