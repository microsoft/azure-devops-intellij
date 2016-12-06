// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.Collections;
import java.util.List;

/**
 * This class represents the results from the the Merge command.
 */
public class MergeResults {
    private final List<MergeMapping> mappings;

    public MergeResults(final List<MergeMapping> mappings) {
        this.mappings = Collections.unmodifiableList(mappings);
    }

    public List<MergeMapping> getMappings() {
        return mappings;
    }

    public boolean noChangesToMerge() {
        return mappings.size() == 0;
    }

    public boolean doConflictsExists() {
        for (final MergeMapping m : mappings) {
            if (m.isConflict()) {
                return true;
            }
        }
        return false;
    }
}
