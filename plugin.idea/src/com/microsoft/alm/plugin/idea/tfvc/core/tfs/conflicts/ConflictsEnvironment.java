// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

public class ConflictsEnvironment {

    private static ConflictsHandler ourConflictsHandler = new DialogConflictsHandler();

// TODO: comment back in once ready to use
//   private static NameMerger ourNameMerger = new DialogNameMerger();
//   private static ContentMerger ourContentMerger = new DialogContentMerger();

//  public static NameMerger getNameMerger() {
//    return ourNameMerger;
//  }
//
//  public static void setNameMerger(NameMerger nameMerger) {
//    ourNameMerger = nameMerger;
//  }
//
//  public static ContentMerger getContentMerger() {
//    return ourContentMerger;
//  }
//
//  public static void setContentMerger(ContentMerger contentMerger) {
//    ourContentMerger = contentMerger;
//  }

    public static void setConflictsHandler(ConflictsHandler conflictsHandler) {
        ourConflictsHandler = conflictsHandler;
    }

    public static ConflictsHandler getConflictsHandler() {
        return ourConflictsHandler;
    }
}
