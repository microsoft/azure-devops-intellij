// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.Collections;
import java.util.List;

/**
 * Types of conflicts found from resolve command
 */
public class ConflictResults {
    final List<String> contentConflicts;
    final List<String> renameConflicts;
    final List<String> bothConflicts;

    public ConflictResults(final List<String> contentConflicts, final List<String> renameConflicts,
                           final List<String> bothConflicts) {
        this.contentConflicts = Collections.unmodifiableList(contentConflicts);
        this.renameConflicts = Collections.unmodifiableList(renameConflicts);
        this.bothConflicts = Collections.unmodifiableList(bothConflicts);
    }

    public List<String> getContentConflicts() {
        return contentConflicts;
    }

    public List<String> getRenameConflicts() {
        return renameConflicts;
    }

    public List<String> getBothConflicts() {
        return bothConflicts;
    }
}