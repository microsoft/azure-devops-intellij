// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

/**
 * MergeConflict holds specific information for a merge operation
 */
public class MergeConflict extends RenameConflict {
    private final MergeMapping mapping;

    public MergeConflict(final String localPath, final MergeMapping mapping) {
        super(localPath, mapping.getToServerItem(), mapping.getFromServerItem(), ConflictType.MERGE);
        this.mapping = mapping;
    }

    public MergeMapping getMapping() {
        return mapping;
    }
}