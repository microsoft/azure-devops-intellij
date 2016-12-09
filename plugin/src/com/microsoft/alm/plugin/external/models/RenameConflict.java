// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

/**
 * RenameConflict holds specific rename information
 */
public class RenameConflict extends Conflict {
    private final String serverPath;
    private final String oldPath;

    public RenameConflict(final String localPath, final String serverPath, final String oldPath) {
        this(localPath, serverPath, oldPath, ConflictType.RENAME);
    }

    public RenameConflict(final String localPath, final String serverPath, final String oldPath, final ConflictType type) {
        super(localPath, type);
        this.serverPath = serverPath;
        this.oldPath = oldPath;
    }

    public String getServerPath() {
        return serverPath;
    }

    public String getOldPath() {
        return oldPath;
    }

    @Override
    public String toString() {
        return "Rename conflict: " + oldPath + " -> " + serverPath + " {" + super.toString() + "}";
    }
}