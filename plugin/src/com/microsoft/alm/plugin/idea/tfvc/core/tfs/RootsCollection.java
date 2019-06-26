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
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.DistinctRootsCollection;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class RootsCollection<T> {

    public static class FilePathRootsCollection extends DistinctRootsCollection<FilePath> {

        public FilePathRootsCollection() {
        }

        public FilePathRootsCollection(final Collection<FilePath> items) {
            super(items);
        }

        @Override
        protected boolean isAncestor(@NotNull FilePath parent, @NotNull FilePath child) {
            return child.isUnder(parent, false);
        }

    }

    public static class ItemPathRootsCollection extends DistinctRootsCollection<ItemPath> {

        public ItemPathRootsCollection() {
        }

        public ItemPathRootsCollection(final Collection<ItemPath> items) {
            super(items);
        }

        @Override
        protected boolean isAncestor(@NotNull ItemPath parent, @NotNull ItemPath child) {
            return child.getLocalPath().isUnder(parent.getLocalPath(), false);
        }

    }

    public static class VirtualFileRootsCollection extends DistinctRootsCollection<VirtualFile> {

        public VirtualFileRootsCollection() {
        }

        public VirtualFileRootsCollection(final Collection<VirtualFile> items) {
            super(items);
        }

        public VirtualFileRootsCollection(final VirtualFile[] items) {
            super(items);
        }

        @Override
        protected boolean isAncestor(@NotNull VirtualFile parent, @NotNull VirtualFile child) {
            return VfsUtil.isAncestor(parent, child, false);
        }

    }

}
