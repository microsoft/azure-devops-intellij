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

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.StatusProvider;
import org.jetbrains.tfsIntegration.core.tfs.StatusVisitor;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

class ChangelistBuilderStatusVisitor implements StatusVisitor {
  private ChangelistBuilder builder;
  private WorkspaceInfo myWorkspace;

  public ChangelistBuilderStatusVisitor(@NotNull ChangelistBuilder builder, final WorkspaceInfo workspace) {
    this.builder = builder;
    myWorkspace = workspace;
  }

  public void unversioned(@NotNull final ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
    }
  }

  public void checkedOutForEdit(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
    throws TfsException {
    if (localItemExists) {
      TFSContentRevision baseRevision = new TFSContentRevision(myWorkspace, extendedItem, false);
      builder.processChange(new Change(baseRevision, CurrentContentRevision.create(path.getLocalPath())));
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }

  public void scheduledForAddition(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      builder.processChange(new Change(null, new CurrentContentRevision(path.getLocalPath())));
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }

  public void scheduledForDeletion(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
    builder.processChange(new Change(new CurrentContentRevision(path.getLocalPath()), null));
  }

  public void outOfDate(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      if (StatusProvider.isFileWritable(path)) {
        builder.processModifiedWithoutCheckout(path.getLocalPath().getVirtualFile());
      }
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }

  public void deleted(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
    }
  }

  public void upToDate(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      if (StatusProvider.isFileWritable(path)) {
        builder.processModifiedWithoutCheckout(path.getLocalPath().getVirtualFile());
      }
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }

  public void renamed(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
    throws TfsException {
    if (localItemExists) {
      ContentRevision before = new TFSContentRevision(myWorkspace, extendedItem, true);
      ContentRevision after = CurrentContentRevision.create(path.getLocalPath());
      builder.processChange(new Change(before, after));
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }

  public void renamedCheckedOut(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
    throws TfsException {
    if (localItemExists) {
      ContentRevision before = new TFSContentRevision(myWorkspace, extendedItem, true);
      ContentRevision after = CurrentContentRevision.create(path.getLocalPath());
      builder.processChange(new Change(before, after));
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }
}
