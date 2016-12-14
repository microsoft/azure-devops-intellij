// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.common.utils.ArgumentHelper;

import java.util.Collections;
import java.util.List;

/**
 * This class represents the results from the the Merge command.
 */
public class MergeResults {
    private final List<MergeMapping> mappings;
    private final List<String> errors;
    private final List<String> warnings;

    public MergeResults() {
        this(Collections.<MergeMapping>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    public MergeResults(final List<MergeMapping> mappings, final List<String> errors, final List<String> warnings) {
        ArgumentHelper.checkNotNull(mappings, "mappings");
        ArgumentHelper.checkNotNull(mappings, "errors");
        ArgumentHelper.checkNotNull(warnings, "warnings");
        this.mappings = Collections.unmodifiableList(mappings);
        this.errors = errors;
        this.warnings = warnings;
    }

    public List<MergeMapping> getMappings() {
        return mappings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean noChangesToMerge() {
        return mappings.size() == 0 && errors.size() == 0 && warnings.size() == 0;
    }

    public boolean doConflictsExists() {
        for (final MergeMapping m : mappings) {
            if (m.isConflict()) {
                return true;
            }
        }
        return false;
    }

    public boolean errorsExist() {
        return errors.size() > 0;
    }

    public boolean warningsExist() {
        return warnings.size() > 0;
    }
}
