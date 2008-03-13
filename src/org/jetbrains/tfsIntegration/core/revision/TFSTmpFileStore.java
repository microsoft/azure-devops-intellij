package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public class TFSTmpFileStore implements TFSContentStore {
  @NonNls private static final String TMP_FILE_NAME = "idea_tfs";
  private static String myTfsTmpDir;

  @Nullable
  public static TFSContentStore find(final FilePath path, final int revision) throws IOException {
    File tmpFile = new File(createTmpFileName(path, revision));
    if (tmpFile.exists()) {
      return new TFSTmpFileStore(tmpFile);
    }
    return null;
  }

  private File myTmpFile;

  TFSTmpFileStore(final FilePath path, final int revision) throws IOException {
    myTmpFile = new File(createTmpFileName(path, revision));
    myTmpFile.deleteOnExit();
  }

  private static String createTmpFileName(final FilePath path, final int revision) throws IOException {
    FilePath parentPath = path.getParentPath();
    String parentPathUrl = parentPath == null ? "" : parentPath.getPresentableUrl();
    return getTfsTmpDir() + File.separator + parentPathUrl.hashCode() + "_" + path.getName() + "." + revision;
  }

  TFSTmpFileStore(final File tmpFile) throws IOException {
    myTmpFile = tmpFile;
  }

  private static String getTfsTmpDir() throws IOException {
    if (myTfsTmpDir == null) {
      File tmpDir = File.createTempFile(TMP_FILE_NAME, "");
      tmpDir.delete();
      tmpDir.mkdir();
      tmpDir.deleteOnExit();
      myTfsTmpDir = tmpDir.getAbsolutePath();
    }
    return myTfsTmpDir;
  }

  public void saveContent(ContentWriter contentWriter) throws IOException {
    OutputStream fileStream = null;
    try {
      fileStream = new FileOutputStream(myTmpFile);
      contentWriter.write(fileStream);
    }
    finally {
      if (fileStream != null) {
        fileStream.close();
      }
    }
  }

  public String loadContent() throws IOException {
    InputStream fileStream = null;
    try {
      fileStream = new FileInputStream(myTmpFile);
      byte [] content = StreamUtil.loadFromStream(fileStream);
      return CharsetToolkit.bytesToString(content);
    }
    finally {
      if (fileStream != null) {
        fileStream.close();
      }
    }
  }
}
