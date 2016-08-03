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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

  private final File myTmpFile;

  TFSTmpFileStore(final String serverUri, final int itemId, final int revision) throws IOException {
    myTmpFile = new File(createTmpFileName(serverUri, itemId, revision));
    myTmpFile.deleteOnExit();
  }

  private static String createTmpFileName(final String serverUri, final int itemId, final int revision) throws IOException {
    return getTfsTmpDir() + File.separator + serverUri.hashCode() + "_" + itemId + "." + revision;
  }

  TFSTmpFileStore(final File tmpFile) {
    myTmpFile = tmpFile;
  }

  private static String getTfsTmpDir() throws IOException {
    if (myTfsTmpDir == null) {
      File tmpDir = FileUtil.createTempFile(TMP_FILE_NAME, "");
      tmpDir.delete();
      tmpDir.mkdir();
      tmpDir.deleteOnExit();
      myTfsTmpDir = tmpDir.getAbsolutePath();
    }
    return myTfsTmpDir;
  }

  public void saveContent(TfsFileUtil.ContentWriter contentWriter) throws TfsException, IOException {
    TfsFileUtil.setFileContent(myTmpFile, contentWriter);
  }

  public byte[] loadContent() throws IOException {
    InputStream fileStream = null;
    try {
      fileStream = new FileInputStream(myTmpFile);
      return StreamUtil.loadFromStream(fileStream);
    }
    finally {
      if (fileStream != null) {
        fileStream.close();
      }
    }
  }
}
