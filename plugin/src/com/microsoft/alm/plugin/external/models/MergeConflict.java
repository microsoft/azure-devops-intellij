// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

/**
 * MergeConflict holds specific information for a merge operation
 */
public class MergeConflict extends RenameConflict {
    private final MergeMapping mapping;

    public MergeConflict(final String localPath, final MergeMapping mapping) {
        // We are treating merge conflicts as renames where needed
        // The name of the file we are branched from is the new name from the the server
        // The name of the file we are branching to is the old name
        // The conflict type is still always MERGE
        super(localPath, mapping.getFromServerItem(), mapping.getToServerItem(), ConflictType.MERGE);
        this.mapping = mapping;
    }

    public MergeMapping getMapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return "Merge conflict " + mapping.toString() + " {" + super.toString() + "}";
    }
}