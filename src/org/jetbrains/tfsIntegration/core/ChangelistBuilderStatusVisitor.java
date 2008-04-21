package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevisionFactory;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

class ChangelistBuilderStatusVisitor implements StatusVisitor {
  private ChangelistBuilder builder;

  public ChangelistBuilderStatusVisitor(@NotNull ChangelistBuilder builder) {
    this.builder = builder;
  }

  public void unversioned(@NotNull final ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
    }
  }

  public void checkedOutForEdit(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
    if (localItemExists) {
      TFSContentRevision baseRevision = TFSContentRevisionFactory.getRevision(path.getLocalPath(), extendedItem.getLver());
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

  public void renamed(@NotNull final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) throws TfsException {
    if (localItemExists) {
      WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(path.getLocalPath());
      FilePath oldPath = workspace.findLocalPathByServerPath(extendedItem.getSitem());
      // TODO getLatest - 1
      TFSContentRevision before = TFSContentRevisionFactory.getRevision(oldPath, extendedItem.getLver() - 1);
      TFSContentRevision after =
        TFSContentRevisionFactory.getRevision(VcsUtil.getFilePath(extendedItem.getLocal()), extendedItem.getLver());
      builder.processChange(new Change(before, after));
    }
    else {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }
  }
}
