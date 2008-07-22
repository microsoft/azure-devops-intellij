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

import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;

import java.io.IOException;

// TODO: implement
public class TFSDocumentUserDataStore implements TFSContentStore {

  public static TFSContentStore find(final FilePath path, final int revision) throws IOException {
    return null;
  }

  TFSDocumentUserDataStore(final FilePath path, final int revision) throws IOException {
  }

  public void saveContent(final TfsFileUtil.ContentWriter contentWriter) throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public String loadContent() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}