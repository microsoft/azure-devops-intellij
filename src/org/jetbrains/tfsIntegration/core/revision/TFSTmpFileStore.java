package org.jetbrains.tfsIntegration.core.revision;

import org.jetbrains.annotations.NonNls;
import com.intellij.openapi.vcs.FilePath;

import java.io.*;

public class TFSTmpFileStore implements TFSContentStore {
  @NonNls private static final String TMP_FILE_NAME = "idea_tfs";
  private static String myTfsTmpDir;

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

  public void saveContent(final String content) throws IOException {
    FileWriter out = new FileWriter(myTmpFile);
    try {
      out.write(content);
    }
    finally {
      out.close();
    }
  }

  public String loadContent() throws IOException {
    StringBuffer sb = new StringBuffer();
    BufferedReader reader = new BufferedReader(new FileReader(myTmpFile));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    }
    finally {
      reader.close();
    }
    return sb.toString();
  }
}
