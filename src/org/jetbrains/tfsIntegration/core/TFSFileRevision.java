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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.io.IOException;
import java.util.Date;

public class TFSFileRevision implements VcsFileRevision {
  private final Date myDate;
  private byte[] myContent;
  private final String myCommitMessage;
  private final String myAuthor;
  private final FilePath myFilePath;
  private final int myChangeset;
  private final WorkspaceInfo myWorkspace;

  public TFSFileRevision(final WorkspaceInfo workspace,
                         final FilePath filePath,
                         final Date date,
                         final String commitMessage,
                         final String author,
                         final int changeset) {
    myWorkspace = workspace;
    myDate = date;
    myCommitMessage = commitMessage;
    myAuthor = author;
    myChangeset = changeset;
    myFilePath = filePath;
  }

  public VcsRevisionNumber getRevisionNumber() {
    return new VcsRevisionNumber.Int(myChangeset);
  }

  public String getBranchName() {
    // TODO
    return null;
  }

  public Date getRevisionDate() {
    return myDate;
  }

  public String getAuthor() {
    return myAuthor;
  }

  public String getCommitMessage() {
    return myCommitMessage;
  }

  public void loadContent() throws VcsException {
    // TODO: encoding
    final String content;
    try {
      content = new TFSContentRevision(myFilePath, myChangeset).getContent();
      myContent = (content != null) ? content.getBytes() : null;
    }
    catch (TfsException e) {
      throw new VcsException("Unable to get revision content for " + myFilePath, e);
    }
  }

  public byte[] getContent() throws IOException {
    return myContent;
  }


}
