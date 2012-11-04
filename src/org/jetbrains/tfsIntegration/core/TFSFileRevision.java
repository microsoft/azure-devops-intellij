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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.TfsRevisionNumber;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.io.IOException;
import java.util.Date;

public class TFSFileRevision implements VcsFileRevision {
  private final Project myProject;
  private final Date myDate;
  private byte[] myContent;
  private final String myCommitMessage;
  private final String myAuthor;
  private final int myItemId;
  private final int myChangeset;
  private final WorkspaceInfo myWorkspace;

  public TFSFileRevision(final Project project,
                         final WorkspaceInfo workspace,
                         final int itemId,
                         final Date date,
                         final String commitMessage,
                         final String author,
                         final int changeset) {
    myProject = project;
    myWorkspace = workspace;
    myDate = date;
    myCommitMessage = commitMessage;
    myAuthor = author;
    myChangeset = changeset;
    myItemId = itemId;
  }

  public VcsRevisionNumber.Int getRevisionNumber() {
    return new TfsRevisionNumber(myChangeset, myItemId);
  }

  public String getBranchName() {
    return null;
  }

  public Date getRevisionDate() {
    return myDate;
  }

  @Override
  public RepositoryLocation getChangedRepositoryPath() {
    return null;
  }

  public String getAuthor() {
    return myAuthor;
  }

  public String getCommitMessage() {
    return myCommitMessage;
  }

  public byte[] loadContent() throws IOException, VcsException {
    // TODO: encoding
    final String content;
    content = createContentRevision().getContent();
    myContent = (content != null) ? content.getBytes() : null;
    return myContent;
  }

  public byte[] getContent() throws IOException, VcsException {
    return myContent;
  }

  public TFSContentRevision createContentRevision() throws VcsException {
    try {
      return TFSContentRevision.create(myProject, myWorkspace, myChangeset, myItemId);
    }
    catch (TfsException e) {
      throw new VcsException("Cannot get revision content", e);
    }
  }
}
