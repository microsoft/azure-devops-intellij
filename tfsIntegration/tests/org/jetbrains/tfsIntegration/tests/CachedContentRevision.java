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

package org.jetbrains.tfsIntegration.tests;

import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public class CachedContentRevision implements ContentRevision {
  private final FilePath myFile;
  private final String myContent;
  private final VcsRevisionNumber myRevisionNumber;
                      

  public CachedContentRevision(ContentRevision contentRevision) throws VcsException {
    myFile = contentRevision.getFile();
    myContent = myFile.isDirectory() ? null : contentRevision.getContent();
    myRevisionNumber = contentRevision.getRevisionNumber();
  }

  @Nullable
  public String getContent() throws VcsException {
    return myContent;
  }

  @NotNull
  public FilePath getFile() {
    return myFile;
  }

  @NotNull
  public VcsRevisionNumber getRevisionNumber() {
    return myRevisionNumber;
  }
}
