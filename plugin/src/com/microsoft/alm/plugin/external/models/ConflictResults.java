// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.List;

/**
 * List of conflicts found from resolve command
 */
public class ConflictResults {
    final List<Conflict> conflicts;

    public ConflictResults(final List<Conflict> conflicts) {
        this.conflicts = conflicts;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }
}