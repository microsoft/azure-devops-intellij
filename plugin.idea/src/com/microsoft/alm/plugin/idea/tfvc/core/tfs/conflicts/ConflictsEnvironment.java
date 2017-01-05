// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
