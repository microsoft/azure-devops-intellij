// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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
