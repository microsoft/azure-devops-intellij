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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.ui.GuiUtils;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// TODO review usage of getFilePath(), getVirtualFile()

public class TfsFileUtil {
    public static final Logger logger = LoggerFactory.getLogger(TfsFileUtil.class);

    public interface ContentWriter {
        void write(OutputStream outputStream) throws TfsException;
    }

    public static boolean isServerItem(final String itemPath) {
        return StringUtils.startsWithIgnoreCase(itemPath, "$/");
    }

    public static List<FilePath> getFilePaths(@NotNull final VirtualFile[] files) {
        return getFilePaths(Arrays.asList(files));
    }

    public static List<FilePath> getFilePaths(@NotNull final Collection<VirtualFile> files) {
        List<FilePath> paths = new ArrayList<FilePath>(files.size());
        for (VirtualFile f : files) {
            paths.add(getFilePath(f));
        }
        return paths;
    }

    public static FilePath getFilePath(@NotNull final VirtualFile f) {
        return VcsContextFactory.SERVICE.getInstance().createFilePathOn(f);
    }

    public static List<String> getFilePathStrings(@NotNull final Collection<VirtualFile> files) {
        List<String> paths = new ArrayList<String>(files.size());
        for (VirtualFile f : files) {
            final FilePath filePath = getFilePath(f);
            paths.add(filePath.getPath());
        }
        return paths;
    }

    public static void setReadOnly(final VirtualFile file, final boolean status) throws IOException {
        setReadOnly(Collections.singletonList(file), status);
    }

    public static void setReadOnly(final Collection<VirtualFile> files, final boolean status) throws IOException {
        final Ref<IOException> exception = new Ref<IOException>();
        try {
            GuiUtils.runOrInvokeAndWait(new Runnable() {
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        public void run() {
                            try {
                                for (VirtualFile file : files) {
                                    ReadOnlyAttributeUtil.setReadOnlyAttribute(file, status);
                                }
                            } catch (IOException e) {
                                exception.set(e);
                            }
                        }
                    });
                }
            });
        } catch (InvocationTargetException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }
        if (!exception.isNull()) {
            throw exception.get();
        }
    }

    private static void setReadOnly(final String path, final boolean status) throws IOException {
        final Ref<IOException> exception = new Ref<IOException>();
        try {
            GuiUtils.runOrInvokeAndWait(new Runnable() {
                public void run() {
                    try {
                        ReadOnlyAttributeUtil.setReadOnlyAttribute(path, status);
                    } catch (IOException e) {
                        exception.set(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }
        if (!exception.isNull()) {
            throw exception.get();
        }
    }

    public static void markFileDirty(final Project project, final @NotNull FilePath file) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                VcsDirtyScopeManager.getInstance(project).fileDirty(file);
            }
        });
    }

    public static void markDirtyRecursively(final Project project, final Collection<FilePath> roots) {
        if (roots.isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                for (FilePath root : roots) {
                    VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(root);
                }
            }
        });
    }

    public static void markDirty(final Project project, final Collection<FilePath> roots, final Collection<FilePath> files) {
        if (roots.isEmpty() && files.isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                for (FilePath root : roots) {
                    VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(root);
                }
                for (FilePath file : files) {
                    VcsDirtyScopeManager.getInstance(project).fileDirty(file);
                }
            }
        });
    }

    public static void markDirtyRecursively(final Project project, final FilePath rootDir) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(rootDir);
            }
        });
    }

    public static void markFileDirty(final Project project, final @NotNull VirtualFile file) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                VcsDirtyScopeManager.getInstance(project).fileDirty(file);
            }
        });
    }

    public static void refreshAndMarkDirty(final Project project, final Collection<VirtualFile> roots, boolean async) {
        refreshAndMarkDirty(project, VfsUtil.toVirtualFileArray(roots), async);
    }

    public static void refreshAndInvalidate(final Project project, final FilePath[] roots, boolean async) {
        VirtualFile[] files = new VirtualFile[roots.length];
        for (int i = 0; i < roots.length; i++) {
            files[i] = roots[i].getVirtualFile();
        }
        refreshAndMarkDirty(project, files, async);
    }

    public static void refreshAndMarkDirty(final Project project, final VirtualFile[] roots, boolean async) {
        RefreshQueue.getInstance().refresh(async, true, new Runnable() {
            public void run() {
                for (VirtualFile root : roots) {
                    try {
                        ArgumentHelper.checkNotNull(root, "root");
                        VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(root);
                    } catch (IllegalArgumentException e) {
                        logger.error("Assertion failure: root is null", e);
                        throw e;
                    } catch (RuntimeException e) {
                        logger.error("RuntimeException while checking for dirty files", e);
                        throw e;
                    }
                }
            }
        }, roots);
    }

    public static void refreshAndFindFile(final FilePath path) {
        try {
            GuiUtils.runOrInvokeAndWait(new Runnable() {
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        public void run() {
                            VirtualFileManager.getInstance().refreshAndFindFileByUrl(path.getPath());
                        }
                    });
                }
            });
        } catch (InvocationTargetException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public static void setFileContent(final @NotNull File destination, final @NotNull ContentWriter contentWriter)
            throws TfsException, IOException, IllegalArgumentException {
        ArgumentHelper.checkIfFile(destination);
        OutputStream fileStream = null;
        try {
            if (destination.exists() && !destination.canWrite()) {
                setReadOnly(destination.getPath(), false);
            }
            fileStream = new FileOutputStream(destination);
            contentWriter.write(fileStream);

            // TODO need this?
            //if (refreshVirtualFile) {
            //  refreshVirtualFileContents(destination);
            //}
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static boolean hasWritableChildFile(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                if ((child.isFile() && child.canWrite()) || hasWritableChildFile(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isFileWritable(FilePath localPath) {
        VirtualFile file = localPath.getVirtualFile();
        return file.isWritable() && !file.isDirectory();
    }

    public static boolean localItemExists(FilePath localPath) {
        VirtualFile file = localPath.getVirtualFile();
        return file != null && file.isValid() && file.exists();
    }

    public static byte[] calculateMD5(File file) throws IOException {
        final MessageDigest digest;
        try {
            //noinspection HardCodedStringLiteral
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return digest.digest();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // skip
                }
            }
        }
    }

    /**
     * Recursively look for files that have a status UNKNOWN
     *
     * @param files
     * @param fileStatusManager
     * @return
     */
    public static boolean findUnknownFiles(final VirtualFile[] files, final FileStatusManager fileStatusManager) {
        for (VirtualFile file : files) {
            // if directory then check children
            if (file.isDirectory()) {
                if (findUnknownFiles(file.getChildren(), fileStatusManager)) {
                    return true;
                }
            } else {
                final FileStatus fileStatus = fileStatusManager.getStatus(file);
                if (fileStatus == FileStatus.UNKNOWN) {
                    return true;
                }
            }
        }
        return false;
    }
}
