package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.peer.PeerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TfsFileUtil {

  public static List<FilePath> getFilePaths(@NotNull final VirtualFile [] files) {
    return getFilePaths(Arrays.asList(files));
  }

  public static List<FilePath> getFilePaths(@NotNull final List<VirtualFile> files) {
    List<FilePath> paths = new ArrayList<FilePath>(files.size());
    for (VirtualFile f : files) {
      paths.add(getFilePath(f));
    }
    return paths;
  }

  public static FilePath getFilePath(@NotNull final VirtualFile f) {
    return PeerFactory.getInstance().getVcsContextFactory().createFilePathOn(f);
  }

}
