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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class VersionControlPath {
    public static final String SERVER_PATH_SEPARATOR = "/";
    public static final String ROOT_FOLDER = "$" + SERVER_PATH_SEPARATOR;

    // TFS does not support unix paths at all so let's pretend we're on windows... (Teamprise does the same)
    private static final String WINDOWS_PATH_SEPARATOR = "\\";
    @SuppressWarnings({"HardCodedStringLiteral"})
    private static final String FAKE_DRIVE_PREFIX = "U:";

    private static final Logger ourLogger = Logger.getInstance(VersionControlPath.class);

    public static String toTfsRepresentation(@Nullable String localPath) {
        if (localPath == null) {
            return null;
        }
        localPath = localPath.replace("/", WINDOWS_PATH_SEPARATOR);
        return SystemInfo.isWindows ? localPath : FAKE_DRIVE_PREFIX + localPath;
    }

    public static String toTfsRepresentation(@NotNull FilePath localPath) {
        return toTfsRepresentation(localPath.getPath());
    }

    private static String canonicalizePath(String path)  {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            ourLogger.warn("Cannot canonicalize path " + path, e);
            return path;
        }
    }

    @Nullable
    public static String localPathFromTfsRepresentation(@Nullable String localPath) {
        if (localPath == null) {
            return null;
        }

        final String systemDependent = FileUtil.toSystemDependentName(localPath);
        if (!SystemInfo.isWindows && systemDependent.startsWith(FAKE_DRIVE_PREFIX)) {
            return canonicalizePath(systemDependent.substring(FAKE_DRIVE_PREFIX.length()));
        } else {
            return canonicalizePath(systemDependent);
        }
    }

    @Nullable
    public static FilePath getFilePath(@Nullable String localPath, boolean isDirectory) {
        return localPath != null ? VcsUtil.getFilePath(localPathFromTfsRepresentation(localPath), isDirectory) : null;
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull String localPath) {
        return VcsUtil.getVirtualFile(localPathFromTfsRepresentation(localPath));
    }

    public static File getFile(String localPath) {
        return new File(localPathFromTfsRepresentation(localPath));
    }

    public static String getPathToProject(final String serverPath) {
        int secondSlashPos = serverPath.indexOf("/", ROOT_FOLDER.length());
        return secondSlashPos == -1 ? serverPath : serverPath.substring(0, secondSlashPos);
    }

    public static String getTeamProject(final String serverPath) {
        int secondSlashPos = serverPath.indexOf("/", ROOT_FOLDER.length());
        return serverPath.substring(ROOT_FOLDER.length(), secondSlashPos != -1 ? secondSlashPos : serverPath.length());
    }

    public static boolean isUnder(String parent, String child) {
        parent = parent.toLowerCase();
        return parent.equals(getCommonAncestor(parent, child.toLowerCase()));
    }

    /**
     * Not to be used for UI because files and subfolders at one level are compared lexicographically and may be mixed.
     *
     * @see #compareParentToChild(String, boolean, String, boolean)
     */
    public static int compareParentToChild(String path1, String path2) {
        return path1.compareTo(path2);
    }

    /**
     * At the same level files go before subfolders regardless of the names.
     */
    public static int compareParentToChild(@NotNull String path1, boolean isDirectory1, @NotNull String path2, boolean isDrectory2) {
        String[] pathComponents1 = getPathComponents(path1);
        String[] pathComponents2 = getPathComponents(path2);

        final int minLength = Math.min(pathComponents1.length, pathComponents2.length);

        // first compare all the levels except last one
        for (int i = 0; i < minLength - 1; i++) {
            String s1 = pathComponents1[i];
            String s2 = pathComponents2[i];
            if (!s1.equals(s2)) {
                return s1.compareTo(s2);
            }
        }

        // compare last level
        if (pathComponents1.length == pathComponents2.length) {
            if (isDirectory1 == isDrectory2) {
                return pathComponents1[pathComponents1.length - 1].compareTo(pathComponents2[pathComponents2.length - 1]);
            } else {
                return isDirectory1 ? 1 : -1;
            }
        } else {
            if (pathComponents1.length == minLength && !isDirectory1) {
                return -1;
            } else if (pathComponents2.length == minLength && !isDrectory2) {
                return 1;
            } else {
                if (pathComponents1[minLength - 1].equals(pathComponents2[minLength - 1])) {
                    return pathComponents1.length - pathComponents2.length;
                } else {
                    return pathComponents1[minLength - 1].compareTo(pathComponents2[minLength - 1]);
                }
            }
        }
    }

    public static String getCommonAncestor(final @NotNull String path1, final @NotNull String path2) {
        String[] components1 = getPathComponents(path1);
        String[] components2 = getPathComponents(path2);

        int i = 0;
        while (i < Math.min(components1.length, components2.length) && components1[i].equals(components2[i])) {
            i++;
        }
        return i == 1 ? ROOT_FOLDER : StringUtil.join(Arrays.asList(components1).subList(0, i), SERVER_PATH_SEPARATOR);
    }

    public static String getLastComponent(final @NotNull String serverPath) {
        return serverPath.substring(serverPath.lastIndexOf(SERVER_PATH_SEPARATOR) + 1);
    }

    public static String[] getPathComponents(final @NotNull String serverPath) {
        return serverPath.split(SERVER_PATH_SEPARATOR);
    }

    public static String getCombinedServerPath(final FilePath localPathBase, final String serverPathBase, final FilePath localPath) {
        String localPathBaseString = FileUtil.toSystemIndependentName(localPathBase.getPath());
        String localPathString = FileUtil.toSystemIndependentName(localPath.getPath());

        String localPathRemainder = localPathString.substring(localPathBaseString.length());
        if (serverPathBase.endsWith(SERVER_PATH_SEPARATOR) && localPathRemainder.startsWith(SERVER_PATH_SEPARATOR)) {
            localPathRemainder = localPathRemainder.substring(1);
        }
        return serverPathBase + localPathRemainder;
    }

    public static FilePath getCombinedLocalPath(final FilePath localPathBase,
                                                final String serverPathBase,
                                                final String serverPath,
                                                final boolean isDirectory) {
        String serverPathBaseString = FileUtil.toSystemDependentName(serverPathBase);
        String serverPathString = FileUtil.toSystemDependentName(serverPath);
        File localFile = new File(localPathBase.getIOFile(), serverPathString.substring(serverPathBaseString.length()));
        return VcsUtil.getFilePath(localFile, isDirectory);
    }

    public static String getCombinedServerPath(String path, String name) {
        return path + (path.endsWith("/") ? "" : "/") + name;
    }
}
