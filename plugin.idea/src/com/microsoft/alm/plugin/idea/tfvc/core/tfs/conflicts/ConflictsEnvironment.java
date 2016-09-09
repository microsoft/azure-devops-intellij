// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

public class ConflictsEnvironment {

    private static ConflictsHandler conflictsHandler = new DialogConflictsHandler();

    private static NameMerger nameMerger = new DialogNameMerger();
    private static ContentMerger contentMerger = new DialogContentMerger();

    public static NameMerger getNameMerger() {
        return nameMerger;
    }

    public static void setNameMerger(NameMerger nameMerger) {
        ConflictsEnvironment.nameMerger = nameMerger;
    }

    public static ContentMerger getContentMerger() {
        return contentMerger;
    }

    public static void setContentMerger(ContentMerger contentMerger) {
        ConflictsEnvironment.contentMerger = contentMerger;
    }

    public static void setConflictsHandler(ConflictsHandler conflictsHandler) {
        ConflictsEnvironment.conflictsHandler = conflictsHandler;
    }

    public static ConflictsHandler getConflictsHandler() {
        return conflictsHandler;
    }
}
