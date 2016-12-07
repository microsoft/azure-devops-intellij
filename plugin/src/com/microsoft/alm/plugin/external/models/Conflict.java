// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

/**
 * Conflict object
 */
public class Conflict {
    public enum ConflictType {CONTENT, RENAME, NAME_AND_CONTENT, MERGE, RESOLVED}

    private final String localPath;
    private final ConflictType type;

    public Conflict(final String localPath, final ConflictType type) {
        this.localPath = localPath;
        this.type = type;
    }

    public String getLocalPath() {
        return localPath;
    }

    public ConflictType getType() {
        return type;
    }
}