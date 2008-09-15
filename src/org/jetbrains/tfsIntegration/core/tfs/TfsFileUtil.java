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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.peer.PeerFactory;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.FileOperationException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.TfsExceptionManager;

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// TODO review usage of getFilePath(), getVirtualFile()

public class TfsFileUtil {
  public interface ContentWriter {
    void write(OutputStream outputStream) throws TfsException;
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
    return PeerFactory.getInstance().getVcsContextFactory().createFilePathOn(f);
  }

  public static void setReadOnlyInEventDispathThread(final VirtualFile file, final boolean status) {
    executeInEventDispatchThread(new Runnable() {
      public void run() {
        try {
          ReadOnlyAttributeUtil.setReadOnlyAttribute(file, status);
        }
        catch (IOException e) {
          TFSVcs.LOG.error(e);
        }
      }
    });
  }

  public static void setReadOnlyInEventDispatchThread(final String path, final boolean status) {
    executeInEventDispatchThread(new Runnable() {
      public void run() {
        try {
          ReadOnlyAttributeUtil.setReadOnlyAttribute(path, status);
        }
        catch (IOException e) {
          TFSVcs.LOG.error(e);
        }
      }
    });
  }

  public static void invalidateFiles(final Project project, final Collection<FilePath> files) {
    if (files.isEmpty()) {
      return;
    }

    final VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        for (FilePath file : files) {
          dirtyScopeManager.fileDirty(file);
        }
      }
    });
  }

  public static void invalidateFile(final Project project, final FilePath file) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        VcsDirtyScopeManager.getInstance(project).fileDirty(file);
      }
    });
  }

  public static void invalidateRecursively(final Project project, final Collection<FilePath> roots) {
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

  public static void invalidate(final Project project, final Collection<FilePath> roots, final Collection<FilePath> files) {
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

  public static void invalidateRecursively(final Project project, final FilePath rootDir) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(rootDir);
      }
    });
  }

  public static void invalidateFile(final Project project, final VirtualFile file) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        VcsDirtyScopeManager.getInstance(project).fileDirty(file);
      }
    });
  }

  public static void refreshVirtualFileContents(final File file) {
    final VirtualFile virtualFile = VcsUtil.getVirtualFile(file);
    final VirtualFile fileToRefresh = virtualFile != null ? virtualFile : VcsUtil.getVirtualFile(file.getParentFile());

    if (fileToRefresh != null) {
      executeInEventDispatchThread(new Runnable() {
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
              fileToRefresh.refresh(false, false);
            }
          });
        }
      });
    }
  }

  public static void executeInEventDispatchThread(Runnable runnable) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runnable.run();
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.defaultModalityState());
    }
  }

  public static void refreshAndInvalidate(final Project project, final Collection<VirtualFile> roots, boolean async) {
    refreshAndInvalidate(project, roots.toArray(new VirtualFile[roots.size()]), async);
  }

  public static void refreshAndInvalidate(final Project project, final FilePath[] roots, boolean async) {
    VirtualFile[] files = new VirtualFile[roots.length];
    for (int i = 0; i < roots.length; i++) {
      files[i] = roots[i].getVirtualFile();
    }
    refreshAndInvalidate(project, files, async);
  }

  public static void refreshAndInvalidate(final Project project, final VirtualFile[] roots, boolean async) {
    RefreshQueue.getInstance().refresh(async, true, new Runnable() {
      public void run() {
        for (VirtualFile root : roots) {
          try {
            TFSVcs.assertTrue(root != null);
            VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(root);
          }
          catch (RuntimeException e) {
            TFSVcs.error("Error in refresh delegate: " + e);
          }
        }
      }
    }, roots);
  }

  public static void refreshRecursively(final VirtualFile parent) {
    executeInEventDispatchThread(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            parent.refresh(false, true);
          }
        });
      }
    });
  }


  public static void setFileContent(final @NotNull File destination, final @NotNull ContentWriter contentWriter) throws TfsException {
    TFSVcs.assertTrue(!destination.isDirectory(), destination + " expected to be a file");
    OutputStream fileStream = null;
    try {
      if (destination.exists() && !destination.canWrite()) {
        setReadOnlyInEventDispatchThread(destination.getPath(), false);
      }
      fileStream = new FileOutputStream(destination);
      contentWriter.write(fileStream);

      // TODO need this?
      //if (refreshVirtualFile) {
      //  refreshVirtualFileContents(destination);
      //}
    }
    catch (FileNotFoundException e) {
      String errorMessage = MessageFormat.format("Failed to create file ''{0}''", destination.getPath());
      throw new FileOperationException(errorMessage);
    }
    catch (IOException e) {
      throw TfsExceptionManager.processException(e);
    }
    finally {
      if (fileStream != null) {
        try {
          fileStream.close();
        }
        catch (IOException e) {
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


}
