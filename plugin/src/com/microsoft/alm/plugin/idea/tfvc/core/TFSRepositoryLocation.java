// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.external.models.Workspace;

public class TFSRepositoryLocation implements RepositoryLocation {

    private final Workspace workspace;
    private final VirtualFile root;

    public TFSRepositoryLocation(final Workspace workspace, final VirtualFile root) {
        this.workspace = workspace;
        this.root = root;
    }

    public VirtualFile getRoot() {
        return root;
    }

    public String getKey() {
        return toString();
    }

    @Override
    public void onBeforeBatch() throws VcsException {
    }

    @Override
    public void onAfterBatch() {
    }

    public String toPresentableString() {
        return workspace.getServer();
    }

    public String toString() {
        return toPresentableString();
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}