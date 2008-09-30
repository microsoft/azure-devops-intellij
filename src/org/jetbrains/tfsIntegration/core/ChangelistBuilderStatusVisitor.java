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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.ServerStatus;
import org.jetbrains.tfsIntegration.core.tfs.StatusVisitor;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

class ChangelistBuilderStatusVisitor implements StatusVisitor {
  private @NotNull final ChangelistBuilder myChangelistBuilder;
  private @NotNull final WorkspaceInfo myWorkspace;

  public ChangelistBuilderStatusVisitor(final @NotNull ChangelistBuilder changelistBuilder, final @NotNull WorkspaceInfo workspace) {
    myChangelistBuilder = changelistBuilder;
    myWorkspace = workspace;
  }

  public void unversioned(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
    if (localItemExists) {
      myChangelistBuilder.processUnversionedFile(localPath.getVirtualFile());
    }
  }

  public void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
    throws TfsException {
    if (localItemExists) {
      TFSContentRevision baseRevision = TFSContentRevision.create(myWorkspace, localPath, serverStatus.localVer, serverStatus.itemId);
      myChangelistBuilder.processChange(new Change(baseRevision, CurrentContentRevision.create(localPath)));
    }
    else {
      myChangelistBuilder.processLocallyDeletedFile(localPath);
    }
  }

  public void scheduledForAddition(final @NotNull FilePath localPath,
                                   final boolean localItemExists,
                                   final @NotNull ServerStatus serverStatus) {
    if (localItemExists) {
      myChangelistBuilder.processChange(new Change(null, new CurrentContentRevision(localPath)));
    }
    else {
      myChangelistBuilder.processLocallyDeletedFile(localPath);
    }
  }

  public void scheduledForDeletion(final @NotNull FilePath localPath,
                                   final boolean localItemExists,
                                   final @NotNull ServerStatus serverStatus) {
    TFSContentRevision baseRevision = TFSContentRevision.create(myWorkspace, localPath, serverStatus.localVer, serverStatus.itemId);
    myChangelistBuilder.processChange(new Change(baseRevision, null));
  }

  public void outOfDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatusm) {
    if (localItemExists) {
      if (TfsFileUtil.isFileWritable(localPath)) {
        myChangelistBuilder.processModifiedWithoutCheckout(localPath.getVirtualFile());
      }
    }
    else {
      myChangelistBuilder.processLocallyDeletedFile(localPath);
    }
  }

  public void deleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
    if (localItemExists) {
      myChangelistBuilder.processUnversionedFile(localPath.getVirtualFile());
    }
  }

  public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
    if (localItemExists) {
      if (TfsFileUtil.isFileWritable(localPath)) {
        myChangelistBuilder.processModifiedWithoutCheckout(localPath.getVirtualFile());
      }
    }
    else {
      myChangelistBuilder.processLocallyDeletedFile(localPath);
    }
  }

  public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
    throws TfsException {
    if (localItemExists) {
      // sourceItem can't be null for renamed
      //noinspection ConstantConditions
      FilePath beforePath = myWorkspace.findLocalPathByServerPath(serverStatus.sourceItem, serverStatus.isDirectory);

      //noinspection ConstantConditions
      TFSContentRevision before = TFSContentRevision.create(myWorkspace, beforePath, serverStatus.localVer, serverStatus.itemId);
      ContentRevision after = CurrentContentRevision.create(localPath);
      myChangelistBuilder.processChange(new Change(before, after));
    }
    else {
      myChangelistBuilder.processLocallyDeletedFile(localPath);
    }
  }

  public void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
    throws TfsException {
    if (localItemExists) {
      // sourceItem can't be null for renamed and checked out for edit
      //noinspection ConstantConditions
      FilePath beforePath = myWorkspace.findLocalPathByServerPath(serverStatus.sourceItem, serverStatus.isDirectory);

      //noinspection ConstantConditions
      TFSContentRevision before = TFSContentRevision.create(myWorkspace, beforePath, serverStatus.localVer, serverStatus.itemId);
      ContentRevision after = CurrentContentRevision.create(localPath);
      myChangelistBuilder.processChange(new Change(before, after));
    }
    else {
      myChangelistBuilder.processLocallyDeletedFile(localPath);
    }
  }

  public void undeleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
    throws TfsException {
    checkedOutForEdit(localPath, localItemExists, serverStatus);
  }

}
