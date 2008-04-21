package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.peer.PeerFactory;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSVcs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

  private static void executeInEventDispatchThread(Runnable runnable) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runnable.run();
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.defaultModalityState());
    }
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
}
