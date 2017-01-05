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

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;

public interface StatusVisitor {

    void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void locked(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void scheduledForAddition(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void scheduledForDeletion(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus);

    void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void unversioned(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void undeleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    /* TODO:
    void outOfDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;
    */
}
