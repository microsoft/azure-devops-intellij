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

package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public class TFSTmpFileStore implements TFSContentStore {
  @NonNls private static final String TMP_FILE_NAME = "idea_tfs";
  private static String myTfsTmpDir;

  @Nullable
  public static TFSContentStore find(final String serverUri, final int itemId, final int revision) throws IOException {
    File tmpFile = new File(createTmpFileName(serverUri, itemId, revision));
    if (tmpFile.exists()) {
      return new TFSTmpFileStore(tmpFile);
    }
    return null;
  }

  private File myTmpFile;

  TFSTmpFileStore(final String serverUri, final int itemId, final int revision) throws IOException {
    myTmpFile = new File(createTmpFileName(serverUri, itemId, revision));
    myTmpFile.deleteOnExit();
  }

  private static String createTmpFileName(final String serverUri, final int itemId, final int revision) throws IOException {
    return getTfsTmpDir() + File.separator + serverUri.hashCode() + "_" + itemId + "." + revision;
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
