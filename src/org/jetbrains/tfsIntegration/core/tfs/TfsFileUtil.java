package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.peer.PeerFactory;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSVcs;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TfsFileUtil {

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

  public static final Comparator<FilePath> PATH_COMPARATOR = new Comparator<FilePath>() {
    public int compare(final FilePath o1, final FilePath o2) {
      return o1.getPath().compareTo(o2.getPath());
    }
  };

}
