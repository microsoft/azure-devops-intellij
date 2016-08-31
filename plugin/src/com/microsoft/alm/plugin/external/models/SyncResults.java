// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.intellij.openapi.vcs.VcsException;

import java.util.Collections;
import java.util.List;

/**
 * Model for the results from the sync command
 */
public class SyncResults {
    private final boolean conflictsExist;
    private final List<String> updatedFiles;
    private final List<String> newFiles;
    private final List<String> deletedFiles;
    private final List<VcsException> exceptions;

    public SyncResults(final boolean conflictsExist, final List<String> updatedFiles, final List<String> newFiles,
                       final List<String> deletedFiles, final List<VcsException> exceptions) {
        this.conflictsExist = conflictsExist;
        this.updatedFiles = Collections.unmodifiableList(updatedFiles);
        this.newFiles = Collections.unmodifiableList(newFiles);
        this.deletedFiles = Collections.unmodifiableList(deletedFiles);
        this.exceptions = Collections.unmodifiableList(exceptions);
    }

    public boolean doConflictsExists() {
        return conflictsExist;
    }

    public List<String> getNewFiles() {
        return newFiles;
    }

    public List<String> getUpdatedFiles() {
        return updatedFiles;
    }

    public List<String> getDeletedFiles() {
        return deletedFiles;
    }

    public List<VcsException> getExceptions() {
        return exceptions;
    }
}
