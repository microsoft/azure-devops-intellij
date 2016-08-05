// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;

public class ItemPath {

    @NotNull
    private final FilePath myLocalPath;
    private final String myServerPath;

    public ItemPath(final @NotNull FilePath localPath, String serverPath) {
        myLocalPath = localPath;
        myServerPath = serverPath;
    }

    @NotNull
    public FilePath getLocalPath() {
        return myLocalPath;
    }

    public String getServerPath() {
        return myServerPath;
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ItemPath itemPath = (ItemPath) o;

        return FileUtil.pathsEqual(getLocalPath().getPath(), itemPath.getLocalPath().getPath());
    }

    public int hashCode() {
        return getLocalPath().hashCode();
    }

    public String toString() {
        return "local: " + getLocalPath() + ", server: " + getServerPath();
    }
}
